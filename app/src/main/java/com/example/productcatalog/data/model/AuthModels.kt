
package com.example.productcatalog.data.model

data class LoginRequest(
    val username: String,
    val password: String,
    val expiresInMins: Int = 60
)

data class LoginResponse(
    val accessToken: String?,
    val refreshToken: String?
)

data class AuthUser(
    val id: Int,
    val username: String
)
