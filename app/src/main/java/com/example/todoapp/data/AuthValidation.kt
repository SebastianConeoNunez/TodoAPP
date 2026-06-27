package com.example.todoapp.data

import android.util.Patterns

object AuthValidation {
    private val EMAIL_PATTERN = Patterns.EMAIL_ADDRESS

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "El correo electrónico es obligatorio"
            !EMAIL_PATTERN.matcher(email).matches() -> "Formato de correo electrónico inválido"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "La contraseña es obligatoria"
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
    }
}
