
package com.example.productcatalog.data.repository

import com.example.productcatalog.data.model.*
import com.example.productcatalog.data.remote.DummyJsonApi
import java.io.IOException

class ProductRepository(
    private val api: DummyJsonApi
) {

    suspend fun getCategories(): List<ProductCategory> {
        return api.getCategories()
    }

    suspend fun getPage(
        categorySlug: String?,
        skip: Int,
        limit: Int
    ): ProductPage {
        return if (categorySlug.isNullOrBlank()) {
            api.getProducts(limit, skip)
        } else {
            api.getProductsByCategory(
                categorySlug,
                limit,
                skip
            )
        }
    }

    suspend fun addProduct(
        request: ProductWriteRequest
    ): Product {
        val response = api.addProduct(request)

        if (!response.isSuccessful) {
            throw IOException(
                "Add failed: HTTP ${response.code()}"
            )
        }

        return response.body()
            ?: throw IOException("Empty add response")
    }

    suspend fun updateProduct(
        id: Int,
        request: ProductWriteRequest
    ): Product {
        val response = api.updateProduct(id, request)

        if (!response.isSuccessful) {
            throw IOException(
                "Update failed: HTTP ${response.code()}"
            )
        }

        val product = response.body()
            ?: throw IOException("Empty update response")

        if (product.id != id) {
            throw IOException("Unexpected updated product ID")
        }

        return product
    }

    suspend fun deleteProduct(id: Int) {
        val response = api.deleteProduct(id)

        if (!response.isSuccessful) {
            throw IOException(
                "Delete failed: HTTP ${response.code()}"
            )
        }

        val result = response.body()

        if (result?.id != id || !result.isDeleted) {
            throw IOException(
                "Server did not confirm deletion"
            )
        }
    }
}
