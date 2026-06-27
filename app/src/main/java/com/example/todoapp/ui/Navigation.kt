package com.example.todoapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.todoapp.viewmodel.AuthState
import com.example.todoapp.viewmodel.AuthViewModel
import com.example.todoapp.viewmodel.FormViewModel
import com.example.todoapp.viewmodel.TaskViewModel

object Routes {
    const val AUTH = "auth_screen"
    const val MAIN = "main_screen"
    const val FORM = "form_screen"
}

private var lastNavigationTime = 0L

fun NavController.navigateOnce(route: String) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastNavigationTime > 500) {
        lastNavigationTime = currentTime
        navigate(route)
    }
}

@Composable
fun TodoAppNavHost(
    navController: NavHostController,
    taskViewModel: TaskViewModel,
    formViewModel: FormViewModel,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    // React to auth state changes with navigation side effects
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // If we're on AUTH screen, navigate to MAIN
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == Routes.AUTH) {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // If we're NOT on AUTH screen, navigate to AUTH
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Routes.AUTH) {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Loading -> { /* wait */ }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToForm = {
                    formViewModel.resetForm()
                    navController.navigateOnce(Routes.FORM)
                },
                onEditTask = { task ->
                    formViewModel.loadTask(task)
                    navController.navigateOnce(Routes.FORM)
                },
                onLogout = {
                    authViewModel.logout()
                },
                viewModel = taskViewModel
            )
        }

        composable(Routes.FORM) {
            FormScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = formViewModel
            )
        }
    }
}
