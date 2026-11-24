
package com.example.productcatalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productcatalog.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isRestoring: Boolean = true,
    val isBusy: Boolean = false,
    val username: String? = null,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState()
    )

    val state = _state.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            try {
                val username = repository.restoreSession()

                _state.value = AuthUiState(
                    isRestoring = false,
                    username = username
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = AuthUiState(
                    isRestoring = false,
                    error = "Session could not be verified. Please sign in."
                )
            }
        }
    }

    fun login(
        username: String,
        password: String
    ) {
        if (_state.value.isBusy ||
            _state.value.isRestoring
        ) return

        if (username.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(
                isRestoring = false,
                error = "Please enter username and password"
            )
            return
        }

        viewModelScope.launch {
            _state.value = AuthUiState(
                isRestoring = false,
                isBusy = true
            )

            try {
                val result = repository.login(
                    username.trim(),
                    password
                )

                _state.value = AuthUiState(
                    isRestoring = false,
                    username = result
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AuthUiState(
                    isRestoring = false,
                    error = e.message
                        ?: "Unable to sign in"
                )
            }
        }
    }

    fun logout() {
        if (_state.value.isBusy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isBusy = true,
                error = null
            )

            try {
                repository.logout()

                _state.value = AuthUiState(
                    isRestoring = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isBusy = false,
                    error = "Unable to sign out"
                )
            }
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        require(
            modelClass.isAssignableFrom(
                AuthViewModel::class.java
            )
        )

        return AuthViewModel(repository) as T
    }
}
