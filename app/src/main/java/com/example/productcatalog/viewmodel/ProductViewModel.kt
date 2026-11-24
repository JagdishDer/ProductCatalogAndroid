
package com.example.productcatalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productcatalog.data.model.*
import com.example.productcatalog.data.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategory: String? = null,

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSaving: Boolean = false,

    val deletingIds: Set<Int> = emptySet(),

    val error: String? = null,
    val loadMoreError: String? = null,
    val categoriesError: String? = null,
    val mutationError: String? = null,

    val nextSkip: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true
)

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductUiState())
    val state = _state.asStateFlow()

    private var remoteProducts = emptyList<Product>()

    private val addedProducts = mutableListOf<Product>()
    private val editedProducts = mutableMapOf<Int, Product>()
    private val deletedIds = mutableSetOf<Int>()

    private var nextLocalId = -1

    private var pageJob: Job? = null
    private var categoriesJob: Job? = null
    private var requestVersion = 0
    private var sessionActive = false

    companion object {
        private const val PAGE_SIZE = 20
    }

    // ------------------------------------------------
    // Session management
    // ------------------------------------------------

    fun onAuthenticated() {
        if (sessionActive) return

        sessionActive = true
        loadCategories()
        refresh()
    }

    fun onSignedOut() {
        sessionActive = false
        requestVersion++

        pageJob?.cancel()
        categoriesJob?.cancel()

        remoteProducts = emptyList()
        addedProducts.clear()
        editedProducts.clear()
        deletedIds.clear()

        nextLocalId = -1
        _state.value = ProductUiState()
    }

    // ------------------------------------------------
    // Session overlay
    // ------------------------------------------------

    private fun visibleProducts(): List<Product> {
        val selected = _state.value.selectedCategory

        val remoteIds = remoteProducts
            .mapTo(mutableSetOf()) { it.id }

        // Edited products can move between categories.
        // Keep them visible even when absent from
        // the current server page.
        val pinnedEdits = editedProducts.values
            .filter { it.id !in remoteIds }

        val remoteWithEdits = remoteProducts.map {
            editedProducts[it.id] ?: it
        }

        return (
            addedProducts.asReversed() +
                pinnedEdits +
                remoteWithEdits
            )
            .filter { product ->
                product.id !in deletedIds &&
                    (selected == null ||
                        product.category == selected)
            }
            .distinctBy { it.id }
    }

    private fun publishProducts() {
        _state.value = _state.value.copy(
            products = visibleProducts()
        )
    }

    fun findProduct(id: Int): Product? {
        return addedProducts.firstOrNull { it.id == id }
            ?: editedProducts[id]
            ?: remoteProducts.firstOrNull { it.id == id }
    }

    // ------------------------------------------------
    // Categories
    // ------------------------------------------------

    fun loadCategories() {
        if (!sessionActive) return

        categoriesJob?.cancel()

        _state.value = _state.value.copy(
            categoriesError = null
        )

        categoriesJob = viewModelScope.launch {
            try {
                val result = repository.getCategories()

                if (sessionActive) {
                    _state.value = _state.value.copy(
                        categories = result,
                        categoriesError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionActive) {
                    _state.value = _state.value.copy(
                        categoriesError = errorMessage(e)
                    )
                }
            }
        }
    }

    fun selectCategory(slug: String?) {
        if (!sessionActive) return
        if (_state.value.selectedCategory == slug) return

        _state.value = _state.value.copy(
            selectedCategory = slug
        )

        refresh()
    }

    // ------------------------------------------------
    // Pagination
    // ------------------------------------------------

    fun refresh() {
        if (!sessionActive) return

        requestVersion++
        val version = requestVersion

        pageJob?.cancel()
        remoteProducts = emptyList()

        val selected = _state.value.selectedCategory

        _state.value = _state.value.copy(
            products = visibleProducts(),
            isLoading = true,
            isLoadingMore = false,
            error = null,
            loadMoreError = null,
            nextSkip = 0,
            total = 0,
            hasMore = true
        )

        pageJob = viewModelScope.launch {
            try {
                val page = repository.getPage(
                    categorySlug = selected,
                    skip = 0,
                    limit = PAGE_SIZE
                )

                if (!sessionActive ||
                    version != requestVersion
                ) return@launch

                remoteProducts = page.products.distinctBy {
                    it.id
                }

                val next = page.skip + page.products.size

                _state.value = _state.value.copy(
                    isLoading = false,
                    nextSkip = next,
                    total = page.total,
                    hasMore = page.products.isNotEmpty() &&
                        next < page.total
                )

                publishProducts()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionActive &&
                    version == requestVersion
                ) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessage(e)
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _state.value

        if (!sessionActive ||
            current.isLoading ||
            current.isLoadingMore ||
            !current.hasMore ||
            current.error != null ||
            current.loadMoreError != null
        ) return

        val version = requestVersion

        _state.value = current.copy(
            isLoadingMore = true
        )

        pageJob = viewModelScope.launch {
            try {
                val page = repository.getPage(
                    categorySlug = current.selectedCategory,
                    skip = current.nextSkip,
                    limit = PAGE_SIZE
                )

                if (!sessionActive ||
                    version != requestVersion
                ) return@launch

                remoteProducts = (
                    remoteProducts + page.products
                    ).distinctBy { it.id }

                val next = page.skip + page.products.size

                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    loadMoreError = null,
                    nextSkip = next,
                    total = page.total,
                    hasMore = page.products.isNotEmpty() &&
                        next < page.total
                )

                publishProducts()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionActive &&
                    version == requestVersion
                ) {
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        loadMoreError = errorMessage(e)
                    )
                }
            }
        }
    }

    fun retryLoadMore() {
        _state.value = _state.value.copy(
            loadMoreError = null
        )
        loadNextPage()
    }

    // ------------------------------------------------
    // Add and Edit
    // ------------------------------------------------

   
    fun saveProduct(
        editingId: Int?,
        request: ProductWriteRequest,
        imageUri: String?,
        onSuccess: () -> Unit
    ) {
        if (!sessionActive || _state.value.isSaving) {
            return
        }

        val title = request.title.trim()
        val category = request.category.trim()

        if (title.isBlank() || category.isBlank()) {
            _state.value = _state.value.copy(
                mutationError = "Name and category are required"
            )
            return
        }

        val cleanRequest = request.copy(
            title = title,
            category = category,
            description = request.description.trim()
        )

        // Only locally stored images are attached to
        // the session product. No image is uploaded.
        val selectedLocalImage = imageUri?.takeIf {
            it.startsWith("file://") ||
                it.startsWith("content://")
        }

        _state.value = _state.value.copy(
            isSaving = true,
            mutationError = null
        )

        viewModelScope.launch {
            try {
                if (editingId == null) {

                    // Create through DummyJSON and retain the
                    // returned product in our local session.
                    val returned = repository.addProduct(
                        cleanRequest
                    )

                    if (!sessionActive) return@launch

                    val local = returned.copy(
                        id = nextLocalId--,
                        title = returned.title.ifBlank {
                            cleanRequest.title
                        },
                        description = returned.description
                            ?: cleanRequest.description,
                        category = returned.category
                            ?: cleanRequest.category,
                        thumbnail = selectedLocalImage
                            ?: returned.thumbnail,
                        isSessionOnly = true,
                        apiId = returned.id
                    )

                    addedProducts.add(local)

                } else {
                    val existing = findProduct(editingId)
                        ?: throw IllegalStateException(
                            "Product not found"
                        )

                    if (existing.isSessionOnly) {
                        // Simulated newly added records cannot
                        // be updated via the server API.
                        val index = addedProducts.indexOfFirst {
                            it.id == editingId
                        }

                        if (index < 0) {
                            throw IllegalStateException(
                                "Local product not found"
                            )
                        }

                        addedProducts[index] = existing.copy(
                            title = cleanRequest.title,
                            description = cleanRequest.description,
                            category = cleanRequest.category,
                            thumbnail = selectedLocalImage
                                ?: existing.thumbnail
                        )

                    } else {
                        // Send text changes to DummyJSON.
                        val returned = repository.updateProduct(
                            existing.id,
                            cleanRequest
                        )

                        if (!sessionActive) return@launch

                        // Keep the local image, because the
                        // API does not upload or persist images.
                        editedProducts[existing.id] =
                            existing.copy(
                                title = returned.title,
                                description = returned.description
                                    ?: cleanRequest.description,
                                category = returned.category
                                    ?: cleanRequest.category,
                                thumbnail = selectedLocalImage
                                    ?: existing.thumbnail
                            )
                    }
                }

                if (!sessionActive) return@launch

                _state.value = _state.value.copy(
                    isSaving = false,
                    mutationError = null
                )

                publishProducts()
                onSuccess()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionActive) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        mutationError = errorMessage(e)
                    )
                }
            }
        }
    }


    // ------------------------------------------------
    // Swipe-to-delete
    // ------------------------------------------------

    fun deleteProduct(id: Int) {
        if (!sessionActive ||
            id in _state.value.deletingIds
        ) return

        val product = findProduct(id) ?: return

        _state.value = _state.value.copy(
            deletingIds = _state.value.deletingIds + id,
            mutationError = null
        )

        viewModelScope.launch {
            try {
                if (product.isSessionOnly) {
                    // Delete a simulated creation locally.
                    addedProducts.removeAll { it.id == id }
                } else {
                    // Remove only after API confirmation.
                    repository.deleteProduct(product.id)

                    if (!sessionActive) return@launch

                    deletedIds.add(product.id)
                    editedProducts.remove(product.id)
                }

                if (!sessionActive) return@launch

                _state.value = _state.value.copy(
                    deletingIds = _state.value.deletingIds - id
                )

                publishProducts()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionActive) {
                    _state.value = _state.value.copy(
                        deletingIds = _state.value.deletingIds - id,
                        mutationError = errorMessage(e)
                    )
                }
            }
        }
    }

    fun clearMutationError() {
        _state.value = _state.value.copy(
            mutationError = null
        )
    }

    private fun errorMessage(error: Throwable): String {
        return error.localizedMessage
            ?: "Something went wrong. Please retry."
    }
}

class ProductViewModelFactory(
    private val repository: ProductRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        require(
            modelClass.isAssignableFrom(
                ProductViewModel::class.java
            )
        )

        return ProductViewModel(repository) as T
    }
}
