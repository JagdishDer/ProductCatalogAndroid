
package com.example.productcatalog.data.model

data class ProductWriteRequest(
    val title: String,
    val description: String,
    val category: String
)

data class DeleteProductResponse(
    val id: Int,
    val isDeleted: Boolean = false,
    val deletedOn: String? = null
)
