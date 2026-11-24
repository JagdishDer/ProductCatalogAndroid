
package com.example.productcatalog.data.remote

import com.example.productcatalog.data.model.AuthUser
import com.example.productcatalog.data.model.LoginRequest
import com.example.productcatalog.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DummyJsonApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<AuthUser>
}
