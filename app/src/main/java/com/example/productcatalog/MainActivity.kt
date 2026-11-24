
package com.example.productcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.productcatalog.data.remote.ApiClient
import com.example.productcatalog.data.repository.AuthRepository
import com.example.productcatalog.data.repository.ProductRepository
import com.example.productcatalog.data.security.SecureTokenStore
import com.example.productcatalog.ui.login.LoginScreen
import com.example.productcatalog.ui.products.ProductListScreen
import com.example.productcatalog.viewmodel.AuthViewModel
import com.example.productcatalog.viewmodel.AuthViewModelFactory
import com.example.productcatalog.viewmodel.ProductViewModel
import com.example.productcatalog.viewmodel.ProductViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Existing Phase 1 authentication
        val authRepository = AuthRepository(
            applicationContext
        )

        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository)
        )[AuthViewModel::class.java]

        // Phase 2 product API
        // Uses the same encrypted token storage as login.
        val tokenStore = SecureTokenStore(
            applicationContext
        )

        val productApi = ApiClient(
            tokenStore
        ).api

        val productRepository = ProductRepository(
            productApi
        )

        val productViewModel = ViewModelProvider(
            this,
            ProductViewModelFactory(productRepository)
        )[ProductViewModel::class.java]

        setContent {
            MaterialTheme {

                val authState by authViewModel.state
                    .collectAsStateWithLifecycle()

                val productState by productViewModel.state
                    .collectAsStateWithLifecycle()

                // Load products after successful authentication.
                // Clear the product session after logout.
                LaunchedEffect(authState.username) {
                    if (authState.username != null) {
                        productViewModel.onAuthenticated()
                    } else {
                        productViewModel.onSignedOut()
                    }
                }

                when {
                    authState.isRestoring -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    authState.username != null -> {
                        ProductListScreen(
                            state = productState,
                            onSelectCategory =
                                productViewModel::selectCategory,
                            onRefresh =
                                productViewModel::refresh,
                            onLoadMore =
                                productViewModel::loadNextPage,
                            onRetryLoadMore =
                                productViewModel::retryLoadMore,
                            onRetryCategories =
                                productViewModel::loadCategories,
                            onLogout =
                                authViewModel::logout
                        )
                    }

                    else -> {
                        LoginScreen(
                            state = authState,
                            onLogin = authViewModel::login
                        )
                    }
                }
            }
        }
    }
}
