
package com.example.productcatalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productcatalog.data.model.Product
import com.example.productcatalog.data.model.ProductCategory
import com.example.productcatalog.data.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ProductUiState(
    val products: List<Product> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategory: String? = null,

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,

    val error: String? = null,
    val loadMoreError: String? = null,
    val categoriesError: String? = null,

    val nextSkip: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true
)

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ProductUiState()
    )
    val state = _state.asStateFlow()

    private var pageJob: Job? = null
    private var categoriesJob: Job? = null

    private var requestVersion = 0
    private var sessionActive = false

    companion object {
        private const val PAGE_SIZE = 20
    }

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

        _state.value = ProductUiState()
    }

    fun loadCategories() {
        if (!sessionActive) return

        categoriesJob?.cancel()

        _state.value = _state.value.copy(
            categoriesError = null
        )

        categoriesJob = viewModelScope.launch {
            try {
                val categories = repository.getCategories()

                if (sessionActive) {
                    _state.value = _state.value.copy(
                        categories = categories,
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

    fun refresh() {
        if (!sessionActive) return

        requestVersion++
        val version = requestVersion

        pageJob?.cancel()

        val selected = _state.value.selectedCategory

        _state.value = _state.value.copy(
            products = emptyList(),
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

                val unique = page.products.distinctBy {
                    it.id
                }

                val next = page.skip + page.products.size

                _state.value = _state.value.copy(
                    products = unique,
                    isLoading = false,
                    total = page.total,
                    nextSkip = next,
                    hasMore = page.products.isNotEmpty() &&
                        next < page.total
                )
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
        val snapshot = _state.value

        if (!sessionActive ||
            snapshot.isLoading ||
            snapshot.isLoadingMore ||
            !snapshot.hasMore ||
            snapshot.error != null ||
            snapshot.loadMoreError != null
        ) return

        val version = requestVersion

        _state.value = snapshot.copy(
            isLoadingMore = true
        )

        pageJob = viewModelScope.launch {
            try {
                val page = repository.getPage(
                    categorySlug = snapshot.selectedCategory,
                    skip = snapshot.nextSkip,
                    limit = PAGE_SIZE
                )

                if (!sessionActive ||
                    version != requestVersion
                ) return@launch

                val current = _state.value
                val ids = current.products
                    .mapTo(mutableSetOf()) { it.id }

                val newProducts = page.products.filter {
                    ids.add(it.id)
                }

                val next = page.skip + page.products.size

                _state.value = current.copy(
                    products = current.products + newProducts,
                    isLoadingMore = false,
                    loadMoreError = null,
                    nextSkip = next,
                    total = page.total,
                    hasMore = page.products.isNotEmpty() &&
                        next < page.total
                )

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

    private fun errorMessage(error: Throwable): String {
        return when (error) {
            is UnknownHostException ->
                "No internet connection. Please try again."

            is SocketTimeoutException ->
                "The request timed out. Please retry."

            is HttpException ->
                "Server error: HTTP ${error.code()}"

            else ->
                error.localizedMessage
                    ?: "Something went wrong."
        }
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
