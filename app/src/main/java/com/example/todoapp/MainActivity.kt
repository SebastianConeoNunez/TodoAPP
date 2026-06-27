package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.todoapp.data.AppDatabase
import com.example.todoapp.data.AuthRepository
import com.example.todoapp.data.LocationService
import com.example.todoapp.repository.TaskRepository
import com.example.todoapp.ui.TodoAppNavHost
import com.example.todoapp.ui.theme.TodoAPPTheme
import com.example.todoapp.viewmodel.AuthViewModel
import com.example.todoapp.viewmodel.FormViewModel
import com.example.todoapp.viewmodel.TaskViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

/**
 * Main entry point of the TodoAPP application.
 *
 * Initializes the Room database, repository, Firebase Authentication,
 * LocationService, and ViewModels, then sets up Compose Navigation via
 * [TodoAppNavHost]. Edge-to-edge rendering is enabled for a modern
 * full-screen experience.
 *
 * The session check uses [FirebaseAuth.currentUser] which resolves
 * immediately from the local cache, satisfying the max 2-second timeout
 * requirement for determining the initial navigation destination.
 *
 * System back-button behaviour is handled automatically by Compose Navigation,
 * which pops the current destination off the back stack (navigating back from
 * the form screen without saving).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())

        // Firebase Auth and AuthRepository
        val firebaseAuth = FirebaseAuth.getInstance()
        val authRepository = AuthRepository(firebaseAuth)

        // AuthViewModel (not user-scoped, manages auth state)
        val authViewModel: AuthViewModel = ViewModelProvider(
            this,
            createFactory { AuthViewModel(authRepository) }
        )[AuthViewModel::class.java]

        // LocationService (not user-scoped)
        val locationService = LocationService(
            LocationServices.getFusedLocationProviderClient(this)
        )

        // User-scoped ViewModels
        val taskViewModel: TaskViewModel = ViewModelProvider(
            this,
            createFactory { TaskViewModel(repository, authRepository) }
        )[TaskViewModel::class.java]

        val formViewModel: FormViewModel = ViewModelProvider(
            this,
            createFactory { FormViewModel(repository, locationService, authRepository) }
        )[FormViewModel::class.java]

        setContent {
            TodoAPPTheme {
                val navController = rememberNavController()
                TodoAppNavHost(
                    navController = navController,
                    taskViewModel = taskViewModel,
                    formViewModel = formViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

/**
 * Creates a [ViewModelProvider.Factory] that delegates ViewModel instantiation
 * to the provided [creator] lambda.
 *
 * This avoids the need for a dependency injection framework while still allowing
 * constructor parameters (such as a repository) to be passed to ViewModels.
 *
 * @param creator A lambda that creates the ViewModel instance.
 * @return A [ViewModelProvider.Factory] wrapping the given creator.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : ViewModel> createFactory(creator: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <V : ViewModel> create(modelClass: Class<V>): V {
            return creator() as V
        }
    }
}
