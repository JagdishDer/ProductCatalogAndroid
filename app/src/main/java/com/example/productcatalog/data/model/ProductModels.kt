
package com.example.productcatalog.data.model

data class Product(
    val id: Int,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val thumbnail: String? = null
)

data class ProductPage(
    val products: List<Product>,
    val total: Int,
    val skip: Int,
    val limit: Int
)

data class ProductCategory(
    val slug: String,
    val name: String,
    val url: String? = null
)
