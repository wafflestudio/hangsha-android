package com.example.hangsha_android.ui.view.signup

import androidx.lifecycle.ViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                signUpMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                signUpMessage = null
            )
        }
    }

    fun onPasswordConfirmationChanged(passwordConfirmation: String) {
        _uiState.update {
            it.copy(
                passwordConfirmation = passwordConfirmation,
                signUpMessage = null
            )
        }
    }

    fun signUp() {
        val currentState = _uiState.value

        if (!currentState.isSubmitEnabled) {
            onSignUpFailure("Please check your email and password inputs.")
            return
        }

        _uiState.update {
            it.copy(
                isSignUpLoading = true,
                signUpMessage = null
            )
        }

        // TODO: Call authRepository.register(...) here and map failures to onSignUpFailure().
    }

    fun onSignUpFailure(message: String) {
        _uiState.update {
            it.copy(
                isSignUpLoading = false,
                signUpMessage = message
            )
        }
    }

    fun onSignUpFailure(error: Throwable) {
        val message = when (error) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is HttpException -> when (error.code()) {
                400 -> "Invalid sign-up request."
                401 -> "Authentication failed while signing up."
                403 -> "You do not have permission to sign up."
                409 -> "This email is already in use."
                in 500..599 -> "Server error occurred. Please try again later."
                else -> "Sign-up failed with code ${error.code()}."
            }
            is IOException -> "Network error occurred. Please try again."
            else -> error.message ?: "An unexpected error occurred during sign-up."
        }

        onSignUpFailure(message)
    }
}
