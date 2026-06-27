package com.example.todoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.data.AuthRepository
import com.example.todoapp.data.LocationService
import com.example.todoapp.data.Task
import com.example.todoapp.repository.TaskRepository
import com.example.todoapp.viewmodel.AuthViewModel
import com.example.todoapp.viewmodel.FormViewModel
import com.example.todoapp.viewmodel.TaskViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for navigation between Main and Form screens.
 * Uses the same FakeTaskDao pattern as ScreenTests.kt.
 *
 * Validates: Requirements 2.1, 4.1, 4.3, 7.1, 7.3
 */
@RunWith(AndroidJUnit4::class)
class NavigationTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockLocationService(): LocationService {
        return LocationService(mockk(relaxed = true))
    }

    private fun createMockAuthRepository(): com.example.todoapp.data.AuthRepository {
        val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        every { mockFirebaseAuth.currentUser } returns mockk(relaxed = true) {
            every { uid } returns "test-user-id"
        }
        return com.example.todoapp.data.AuthRepository(mockFirebaseAuth)
    }

    private fun createMockAuthViewModel(): AuthViewModel {
        return AuthViewModel(createMockAuthRepository())
    }

    /**
     * A FakeTaskDao that tracks insertions to verify that cancel doesn't persist data.
     */
    private class TrackingTaskDao(
        private val tasksFlow: MutableStateFlow<List<Task>> = MutableStateFlow(emptyList())
    ) : com.example.todoapp.data.TaskDao {
        val insertedTasks = mutableListOf<Task>()

        override fun getAllTasks(): Flow<List<Task>> = tasksFlow

        override fun getTasksForUser(userId: String): Flow<List<Task>> = tasksFlow

        override fun getTasksWithLocationForUser(userId: String): Flow<List<Task>> = tasksFlow

        override suspend fun insertTask(task: Task) {
            insertedTasks.add(task)
            tasksFlow.value = tasksFlow.value + task
        }

        override suspend fun deleteTask(task: Task) {
            tasksFlow.value = tasksFlow.value.filter { it.id != task.id }
        }
    }

    @Test
    fun navigation_fromMainToForm_displaysFormScreen() {
        val dao = FakeTaskDao(flow { emit(emptyList()) })
        val repository = TaskRepository(dao)
        val authRepo = createMockAuthRepository()
        val taskViewModel = TaskViewModel(repository, authRepo)
        val locationService = createMockLocationService()
        val formViewModel = FormViewModel(repository, locationService, authRepo)
        val authViewModel = createMockAuthViewModel()

        composeTestRule.setContent {
            val navController = rememberNavController()
            TodoAppNavHost(
                navController = navController,
                taskViewModel = taskViewModel,
                formViewModel = formViewModel,
                authViewModel = authViewModel
            )
        }

        // Main screen should be displayed initially
        composeTestRule.onNodeWithText("TodoAPP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").assertIsDisplayed()

        // Navigate to Form
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").performClick()

        // Wait for navigation to complete
        composeTestRule.waitForIdle()

        // Form screen content should now be visible
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volver").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre de la tarea").assertIsDisplayed()
    }

    @Test
    fun navigation_volverButton_navigatesBack() {
        val dao = FakeTaskDao(flow { emit(emptyList()) })
        val repository = TaskRepository(dao)
        val authRepo = createMockAuthRepository()
        val taskViewModel = TaskViewModel(repository, authRepo)
        val locationService = createMockLocationService()
        val formViewModel = FormViewModel(repository, locationService, authRepo)
        val authViewModel = createMockAuthViewModel()

        composeTestRule.setContent {
            val navController = rememberNavController()
            TodoAppNavHost(
                navController = navController,
                taskViewModel = taskViewModel,
                formViewModel = formViewModel,
                authViewModel = authViewModel
            )
        }

        // Navigate to Form
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").performClick()
        composeTestRule.waitForIdle()

        // Verify we're on the form screen
        composeTestRule.onNodeWithText("Volver").assertIsDisplayed()

        // Click "Volver" to navigate back
        composeTestRule.onNodeWithText("Volver").performClick()
        composeTestRule.waitForIdle()

        // Main screen should be displayed again
        composeTestRule.onNodeWithText("TodoAPP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").assertIsDisplayed()
    }

    @Test
    fun navigation_systemBackButton_navigatesWithoutSaving() {
        val trackingDao = TrackingTaskDao()
        val repository = TaskRepository(trackingDao)
        val authRepo = createMockAuthRepository()
        val taskViewModel = TaskViewModel(repository, authRepo)
        val locationService = createMockLocationService()
        val formViewModel = FormViewModel(repository, locationService, authRepo)
        val authViewModel = createMockAuthViewModel()

        composeTestRule.setContent {
            val navController = rememberNavController()
            TodoAppNavHost(
                navController = navController,
                taskViewModel = taskViewModel,
                formViewModel = formViewModel,
                authViewModel = authViewModel
            )
        }

        // Navigate to Form
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").performClick()
        composeTestRule.waitForIdle()

        // Verify we're on the form screen
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()

        // Press system back button using Espresso
        androidx.test.espresso.Espresso.pressBack()
        composeTestRule.waitForIdle()

        // Main screen should be displayed again
        composeTestRule.onNodeWithText("TodoAPP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").assertIsDisplayed()

        // No tasks should have been saved
        assert(trackingDao.insertedTasks.isEmpty()) {
            "Expected no tasks to be saved after back navigation, but found ${trackingDao.insertedTasks.size}"
        }
    }

    @Test
    fun navigation_debounce_preventsDoubleNavigation() {
        val dao = FakeTaskDao(flow { emit(emptyList()) })
        val repository = TaskRepository(dao)
        val authRepo = createMockAuthRepository()
        val taskViewModel = TaskViewModel(repository, authRepo)
        val locationService = createMockLocationService()
        val formViewModel = FormViewModel(repository, locationService, authRepo)
        val authViewModel = createMockAuthViewModel()

        composeTestRule.setContent {
            val navController = rememberNavController()
            TodoAppNavHost(
                navController = navController,
                taskViewModel = taskViewModel,
                formViewModel = formViewModel,
                authViewModel = authViewModel
            )
        }

        // Rapidly click the navigation button twice
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").performClick()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").performClick()
        composeTestRule.waitForIdle()

        // Form screen should be displayed (single instance)
        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volver").assertIsDisplayed()

        // Press back to verify we return to main directly (no duplicate form in back stack)
        androidx.test.espresso.Espresso.pressBack()
        composeTestRule.waitForIdle()

        // Should be back on main screen directly, not another form screen
        composeTestRule.onNodeWithText("TodoAPP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").assertIsDisplayed()
    }
}
