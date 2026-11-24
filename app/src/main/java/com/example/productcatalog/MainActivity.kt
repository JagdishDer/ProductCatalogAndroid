
package com.example.productcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.productcatalog.ui.editor.ProductEditorScreen
import com.example.productcatalog.viewmodel.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preserve Phase 1 secure authentication
        val authRepository = AuthRepository(
            applicationContext
        )

        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository)
        )[AuthViewModel::class.java]

        // Use the same encrypted token store
        val tokenStore = SecureTokenStore(
            applicationContext
        )

        val productApi = ApiClient(tokenStore).api

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

                var editorOpen by rememberSaveable {
                    mutableStateOf(false)
                }

                var editingId by rememberSaveable {
                    mutableStateOf<Int?>(null)
                }

                LaunchedEffect(authState.username) {
                    if (authState.username != null) {
                        productViewModel.onAuthenticated()
                    } else {
                        productViewModel.onSignedOut()
                        editorOpen = false
                        editingId = null
                    }
                }

                BackHandler(
                    enabled = editorOpen &&
                        !productState.isSaving
                ) {
                    editorOpen = false
                    editingId = null
                    productViewModel.clearMutationError()
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

                    authState.username == null -> {
                        LoginScreen(
                            state = authState,
                            onLogin = authViewModel::login
                        )
                    }

                    editorOpen -> {
                        ProductEditorScreen(
                            product = editingId?.let {
                                productViewModel.findProduct(it)
                            },
                            state = productState,
                            onCancel = {
                                editorOpen = false
                                editingId = null
                                productViewModel.clearMutationError()
                            },
                            onSave = { request ->
                                productViewModel.saveProduct(
                                    editingId = editingId,
                                    request = request,
                                    onSuccess = {
                                        editorOpen = false
                                        editingId = null
                                    }
                                )
                            }
                        )
                    }

                    else -> {
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
                                authViewModel::logout,
                            onAdd = {
                                productViewModel.clearMutationError()
                                editingId = null
                                editorOpen = true
                            },
                            onEdit = { productId ->
                                productViewModel.clearMutationError()
                                editingId = productId
                                editorOpen = true
                            },
                            onDelete =
                                productViewModel::deleteProduct,
                            onClearMutationError =
                                productViewModel::clearMutationError
                        )
                    }
                }
            }
        }
    }
}
