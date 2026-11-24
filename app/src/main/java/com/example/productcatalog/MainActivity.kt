
package com.example.productcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.productcatalog.data.repository.AuthRepository
import com.example.productcatalog.ui.login.LoginScreen
import com.example.productcatalog.ui.login.WelcomeScreen
import com.example.productcatalog.viewmodel.AuthViewModel
import com.example.productcatalog.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AuthRepository(
            applicationContext
        )

        val viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(repository)
        )[AuthViewModel::class.java]

        setContent {
            MaterialTheme {

                val state by viewModel.state
                    .collectAsStateWithLifecycle()

                when {
                    state.isRestoring -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.username != null -> {
                        WelcomeScreen(
                            username = state.username!!,
                            state = state,
                            onLogout = viewModel::logout
                        )
                    }

                    else -> {
                        LoginScreen(
                            state = state,
                            onLogin = viewModel::login
                        )
                    }
                }
            }
        }
    }
}
