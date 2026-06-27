package com.example.todoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.AuthRepository
import com.example.todoapp.data.AuthValidation
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing the authentication state of the application.
 */
sealed class AuthState {
    /** Initial state while checking for an existing session. */
    object Loading : AuthState()

    /** User is authenticated with the given [userId]. */
    data class Authenticated(val userId: String) : AuthState()

    /** No active session exists. */
    object Unauthenticated : AuthState()
}

/**
 * Data class representing the current state of the authentication form.
 *
 * @param email The current email input value.
 * @param password The current password input value.
 * @param emailError Validation error for the email field, or null if valid.
 * @param passwordError Validation error for the password field, or null if valid.
 * @param generalError A general error message (network, Firebase), or null if none.
 * @param isLoading Whether an authentication operation is in progress.
 */
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel responsible for managing authentication state, form validation,
 * and communication with [AuthRepository].
 *
 * Observes [AuthRepository.authStateFlow] to reactively update [authState].
 * Validates form inputs using [AuthValidation] before invoking Firebase operations.
 * Maps Firebase exceptions to user-facing Spanish error messages.
 *
 * @param authRepository The [AuthRepository] used for authentication operations.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)

    /**
     * The current authentication state of the application.
     * Transitions from [AuthState.Loading] to either [AuthState.Authenticated]
     * or [AuthState.Unauthenticated] based on Firebase session status.
     */
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _formState = MutableStateFlow(AuthFormState())

    /**
     * The current state of the authentication form, including input values,
     * validation errors, and loading status.
     */
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    init {
        observeAuthState()
    }

    /**
     * Observes the auth repository's state flow and maps it to [AuthState].
     * Sets the initial state based on whether a current user exists.
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                _authState.value = if (user != null) {
                    AuthState.Authenticated(user.uid)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    /**
     * Updates the email field in the form state, clearing any existing email error.
     *
     * @param email The new email value.
     */
    fun updateEmail(email: String) {
        _formState.value = _formState.value.copy(email = email, emailError = null)
    }

    /**
     * Updates the password field in the form state, clearing any existing password error.
     *
     * @param password The new password value.
     */
    fun updatePassword(password: String) {
        _formState.value = _formState.value.copy(password = password, passwordError = null)
    }

    /**
     * Attempts to register a new user with the current form email and password.
     *
     * Validates input first using [AuthValidation]. If validation fails, sets
     * appropriate errors in [formState] and does NOT call Firebase.
     * On success, [authState] transitions to [AuthState.Authenticated].
     * On failure, maps the Firebase exception to a Spanish error message in [formState].
     */
    fun register() {
        val currentForm = _formState.value
        val emailError = AuthValidation.validateEmail(currentForm.email)
        val passwordError = AuthValidation.validatePassword(currentForm.password)

        if (emailError != null || passwordError != null) {
            _formState.value = currentForm.copy(
                emailError = emailError,
                passwordError = passwordError,
                generalError = null
            )
            return
        }

        _formState.value = currentForm.copy(
            emailError = null,
            passwordError = null,
            generalError = null,
            isLoading = true
        )

        viewModelScope.launch {
            val result = authRepository.register(currentForm.email, currentForm.password)
            result.fold(
                onSuccess = {
                    _formState.value = _formState.value.copy(isLoading = false)
                },
                onFailure = { exception ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        generalError = mapFirebaseException(exception)
                    )
                }
            )
        }
    }

    /**
     * Attempts to sign in with the current form email and password.
     *
     * Validates input first using [AuthValidation]. If validation fails, sets
     * appropriate errors in [formState] and does NOT call Firebase.
     * On success, [authState] transitions to [AuthState.Authenticated].
     * On failure, maps the Firebase exception to a Spanish error message in [formState].
     */
    fun login() {
        val currentForm = _formState.value
        val emailError = AuthValidation.validateEmail(currentForm.email)
        val passwordError = AuthValidation.validatePassword(currentForm.password)

        if (emailError != null || passwordError != null) {
            _formState.value = currentForm.copy(
                emailError = emailError,
                passwordError = passwordError,
                generalError = null
            )
            return
        }

        _formState.value = currentForm.copy(
            emailError = null,
            passwordError = null,
            generalError = null,
            isLoading = true
        )

        viewModelScope.launch {
            val result = authRepository.login(currentForm.email, currentForm.password)
            result.fold(
                onSuccess = {
                    _formState.value = _formState.value.copy(isLoading = false)
                },
                onFailure = { exception ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        generalError = mapFirebaseException(exception)
                    )
                }
            )
        }
    }

    /**
     * Signs out the current user by calling [AuthRepository.logout].
     * The [authState] will transition to [AuthState.Unauthenticated] via the
     * auth state observer.
     */
    fun logout() {
        authRepository.logout()
        _formState.value = AuthFormState()
    }

    /**
     * Clears all error messages from the form state.
     */
    fun clearError() {
        _formState.value = _formState.value.copy(
            emailError = null,
            passwordError = null,
            generalError = null
        )
    }

    /**
     * Maps Firebase authentication exceptions to user-facing Spanish error messages.
     *
     * @param exception The exception thrown by Firebase Auth.
     * @return A localized error message string.
     */
    private fun mapFirebaseException(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> "El correo ya se encuentra en uso"
            is FirebaseAuthInvalidCredentialsException -> "Credenciales incorrectas"
            is FirebaseNetworkException -> "No se pudo conectar al servidor"
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
            else -> "Ocurrió un error inesperado"
        }
    }
}
