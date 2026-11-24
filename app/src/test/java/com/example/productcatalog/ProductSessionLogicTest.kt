package com.example.productcatalog.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSessionLogicTest {

    @Test
    fun `edited and added products survive refresh while deleted products stay hidden`() {
        // Simulate the server returning its original data again after a refresh.
        val remote = listOf(
            Product(id = 1, title = "Old mascara", category = "beauty"),
            Product(id = 2, title = "Eyeshadow", category = "beauty"),
            Product(id = 3, title = "Perfume", category = "fragrances")
        )
        val added = listOf(
            Product(id = -1, title = "New lip balm", category = "beauty", isSessionOnly = true)
        )
        val edited = mapOf(
            1 to Product(id = 1, title = "Updated mascara", category = "beauty")
        )

        val visible = ProductSessionLogic.visibleProducts(
            remoteProducts = remote,
            addedProducts = added,
            editedProducts = edited,
            deletedIds = setOf(2),
            selectedCategory = "beauty"
        )

        assertEquals(listOf(-1, 1), visible.map { it.id })
        assertEquals(listOf("New lip balm", "Updated mascara"), visible.map { it.title })
    }

    @Test
    fun `next pagination offset uses server response size not session row count`() {
        // Even after local delete/filter operations reduce visible rows,
        // GET /products?skip=20&limit=20 must be followed by skip=40.
        assertEquals(40, ProductSessionLogic.nextServerOffset(20, 20))
        assertEquals(7, ProductSessionLogic.nextServerOffset(0, 7))
    }
}
