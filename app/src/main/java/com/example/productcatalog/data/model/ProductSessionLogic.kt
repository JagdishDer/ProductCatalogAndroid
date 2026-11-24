package com.example.productcatalog.data.model

/**
 * Pure, testable session logic used by ProductViewModel.
 * API responses are not persisted by DummyJSON, so session changes
 * must be layered over subsequently fetched server pages.
 */
object ProductSessionLogic {

    fun visibleProducts(
        remoteProducts: List<Product>,
        addedProducts: List<Product>,
        editedProducts: Map<Int, Product>,
        deletedIds: Set<Int>,
        selectedCategory: String?
    ): List<Product> {
        val fetchedIds = remoteProducts.mapTo(mutableSetOf()) { it.id }

        // An edited item may have moved into the selected category,
        // so show edits even if that item isn't in this server page.
        val editsMissingFromPage = editedProducts.values.filter {
            it.id !in fetchedIds
        }

        return (
            addedProducts.asReversed() +
                editsMissingFromPage +
                remoteProducts.map { editedProducts[it.id] ?: it }
            )
            .filter { product ->
                product.id !in deletedIds &&
                    (selectedCategory == null || product.category == selectedCategory)
            }
            .distinctBy { it.id }
    }

    /** Offset must follow the server response, not locally visible row count. */
    fun nextServerOffset(serverSkip: Int, returnedCount: Int): Int {
        require(serverSkip >= 0) { "Server skip must be non-negative" }
        require(returnedCount >= 0) { "Returned count must be non-negative" }
        return serverSkip + returnedCount
    }
}
