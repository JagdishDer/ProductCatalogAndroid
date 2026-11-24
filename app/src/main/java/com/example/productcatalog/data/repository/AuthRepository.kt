
package com.example.productcatalog.data.repository

import android.content.Context
import com.example.productcatalog.data.model.LoginRequest
import com.example.productcatalog.data.remote.ApiClient
import com.example.productcatalog.data.security.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AuthRepository(context: Context) {

    private val tokenStore = SecureTokenStore(context)

    private val api = ApiClient(
        tokenStore
    ).api

    suspend fun login(
        username: String,
        password: String
    ): String = withContext(Dispatchers.IO) {

        val response = api.login(
            LoginRequest(
                username = username,
                password = password
            )
        )

        if (!response.isSuccessful) {
            throw IOException(
                when (response.code()) {
                    400, 401 -> "Invalid username or password"
                    else -> "Login failed: HTTP ${response.code()}"
                }
            )
        }

        val token = response.body()
            ?.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException(
                "Login succeeded but token was missing"
            )

        tokenStore.saveToken(token)

        try {
            val verification = api.getCurrentUser()

            if (!verification.isSuccessful) {
                throw IOException(
                    "Token verification failed"
                )
            }

            verification.body()?.username
                ?: throw IOException(
                    "User information unavailable"
                )
        } catch (e: Exception) {
            tokenStore.clearToken()
            throw e
        }
    }

    suspend fun restoreSession(): String? =
        withContext(Dispatchers.IO) {

            val savedToken = tokenStore.readToken()
                ?: return@withContext null

            val response = api.getCurrentUser()

            if (
                response.code() == 401 ||
                response.code() == 403
            ) {
                tokenStore.clearToken()
                return@withContext null
            }

            if (!response.isSuccessful) {
                throw IOException(
                    "Unable to verify saved session"
                )
            }

            response.body()?.username
                ?: throw IOException(
                    "Invalid user response"
                )
        }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenStore.clearToken()
        }
    }
}
