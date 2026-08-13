package com.cryptowallet.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptowallet.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
)

class AuthViewModel : ViewModel() {

    private val _repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.update {
            it.copy(
                mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                errorMessage = null,
            )
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun submit() {
        val currentState = _uiState.value
        val validationError = validate(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = if (currentState.mode == AuthMode.LOGIN) {
                _repository.login(currentState.email.trim(), currentState.password)
            } else {
                _repository.register(currentState.name.trim(), currentState.email.trim(), currentState.password)
            }

            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = mapError(error)) }
                }
        }
    }

    private fun validate(state: AuthUiState): String? {
        if (state.email.isBlank() || state.password.isBlank()) {
            return "Fill in email and password."
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            return "Enter a valid email."
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters."
        }
        if (state.mode == AuthMode.REGISTER) {
            if (state.name.isBlank()) {
                return "Enter your name."
            }
            if (state.password != state.confirmPassword) {
                return "Passwords don't match."
            }
        }
        return null
    }

    private fun mapError(error: Throwable): String = error.message ?: "Something went wrong. Try again."

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
