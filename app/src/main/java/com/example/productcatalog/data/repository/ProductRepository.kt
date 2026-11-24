
package com.example.productcatalog.data.repository

import com.example.productcatalog.data.model.ProductCategory
import com.example.productcatalog.data.model.ProductPage
import com.example.productcatalog.data.remote.DummyJsonApi

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
            api.getProducts(
                limit = limit,
                skip = skip
            )
        } else {
            api.getProductsByCategory(
                slug = categorySlug,
                limit = limit,
                skip = skip
            )
        }
    }
}
