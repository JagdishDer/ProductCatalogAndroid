
package com.example.productcatalog.data.remote

import com.example.productcatalog.data.model.AuthUser
import com.example.productcatalog.data.model.LoginRequest
import com.example.productcatalog.data.model.LoginResponse
import com.example.productcatalog.data.model.ProductCategory
import com.example.productcatalog.data.model.ProductPage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DummyJsonApi {

    // Authentication — existing Phase 1 endpoints

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<AuthUser>

    // Product list with pagination

    @GET("products")
    suspend fun getProducts(
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductPage

    // All available product categories

    @GET("products/categories")
    suspend fun getCategories(): List<ProductCategory>

    // Products filtered by category

    @GET("products/category/{slug}")
    suspend fun getProductsByCategory(
        @Path("slug") slug: String,
        @Query("limit") limit: Int,
        @Query("skip") skip: Int
    ): ProductPage
}
