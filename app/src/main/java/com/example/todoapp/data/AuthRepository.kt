package com.example.todoapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository that encapsulates Firebase Authentication operations
 * and exposes a clean API to ViewModels.
 *
 * Provides reactive observation of authentication state through [authStateFlow],
 * as well as suspend functions for login, registration, and logout.
 *
 * @param firebaseAuth The [FirebaseAuth] instance used for authentication operations.
 */
class AuthRepository(private val firebaseAuth: FirebaseAuth) {

    /**
     * The currently authenticated user, or null if no user is signed in.
     */
    val currentUser: FirebaseUser? get() = firebaseAuth.currentUser

    private val _authStateFlow = MutableStateFlow(firebaseAuth.currentUser)

    /**
     * A [StateFlow] that emits the current [FirebaseUser] whenever the authentication
     * state changes (sign-in, sign-out, token refresh).
     */
    val authStateFlow: StateFlow<FirebaseUser?> = _authStateFlow.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        _authStateFlow.value = auth.currentUser
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    /**
     * Registers a new user with the given email and password.
     *
     * @param email The email address for the new account.
     * @param password The password for the new account (must be at least 6 characters).
     * @return A [Result] containing the [FirebaseUser] on success, or the exception on failure.
     */
    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Registration succeeded but user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with the given email and password.
     *
     * @param email The email address of the account.
     * @param password The password of the account.
     * @return A [Result] containing the [FirebaseUser] on success, or the exception on failure.
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Login succeeded but user is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user, invalidating the active session.
     */
    fun logout() {
        firebaseAuth.signOut()
    }
}
