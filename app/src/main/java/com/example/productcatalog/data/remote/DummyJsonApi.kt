
package com.example.productcatalog.data.remote

import com.example.productcatalog.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface DummyJsonApi {

    // Authentication
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<AuthUser>

    // Product listing
    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductPage

    // Categories
    @GET("products/categories")
    suspend fun getCategories(): List<ProductCategory>

    // Filtered products
    @GET("products/category/{slug}")
    suspend fun getProductsByCategory(
        @Path("slug") slug: String,
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductPage

    // Add a product
    @POST("products/add")
    suspend fun addProduct(
        @Body request: ProductWriteRequest
    ): Response<Product>

    // Update an existing product
    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body request: ProductWriteRequest
    ): Response<Product>

    // Delete an existing product
    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Int
    ): Response<DeleteProductResponse>
}
