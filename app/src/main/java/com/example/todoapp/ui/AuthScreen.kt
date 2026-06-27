package com.example.todoapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.viewmodel.AuthState
import com.example.todoapp.viewmodel.AuthViewModel

/**
 * Authentication screen composable that provides login and registration functionality.
 *
 * Displays email and password input fields with inline validation errors,
 * "Iniciar Sesión" and "Registrarse" buttons, a loading indicator during
 * authentication operations, and general error messages for network or
 * Firebase errors. User input is preserved on error.
 *
 * Observes [AuthViewModel.authState] and navigates via [onAuthSuccess] when
 * the user transitions to [AuthState.Authenticated].
 *
 * @param authViewModel The [AuthViewModel] managing authentication state and form logic.
 * @param onAuthSuccess Callback invoked when authentication succeeds (state transitions to Authenticated).
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val formState by authViewModel.formState.collectAsState()

    // Navigate when auth state transitions to Authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "TodoAPP",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Inicia sesión o regístrate para continuar",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        OutlinedTextField(
            value = formState.email,
            onValueChange = { authViewModel.updateEmail(it) },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = formState.emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        // Inline email validation error
        if (formState.emailError != null) {
            Text(
                text = formState.emailError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = formState.password,
            onValueChange = { authViewModel.updatePassword(it) },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = formState.passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        // Inline password validation error
        if (formState.passwordError != null) {
            Text(
                text = formState.passwordError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // General error message (network, duplicate email, invalid credentials)
        if (formState.generalError != null) {
            Text(
                text = formState.generalError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loading indicator
        if (formState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // "Iniciar Sesión" button
        Button(
            onClick = { authViewModel.login() },
            enabled = !formState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Registrarse" button
        OutlinedButton(
            onClick = { authViewModel.register() },
            enabled = !formState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Registrarse")
        }
    }
}
