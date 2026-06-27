package com.example.todoapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.data.AuthRepository
import com.example.todoapp.data.LocationService
import com.example.todoapp.data.Task
import com.example.todoapp.data.TaskDao
import com.example.todoapp.repository.TaskRepository
import com.example.todoapp.viewmodel.FormViewModel
import com.example.todoapp.viewmodel.TaskViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fake TaskDao for testing that returns controlled data.
 */
class FakeTaskDao(
    private val tasksFlow: Flow<List<Task>> = flow { emit(emptyList()) }
) : TaskDao {

    override fun getAllTasks(): Flow<List<Task>> = tasksFlow

    override fun getTasksForUser(userId: String): Flow<List<Task>> = tasksFlow

    override fun getTasksWithLocationForUser(userId: String): Flow<List<Task>> = tasksFlow

    override suspend fun insertTask(task: Task) {
        // No-op for UI tests
    }

    override suspend fun deleteTask(task: Task) {
        // No-op for UI tests
    }
}

/**
 * Fake TaskDao that throws an exception when getAllTasks is collected,
 * used to simulate a load failure in the UI.
 */
class ErrorTaskDao : TaskDao {

    override fun getAllTasks(): Flow<List<Task>> = flow {
        throw RuntimeException("No se pudieron cargar las tareas")
    }

    override fun getTasksForUser(userId: String): Flow<List<Task>> = flow {
        throw RuntimeException("No se pudieron cargar las tareas")
    }

    override fun getTasksWithLocationForUser(userId: String): Flow<List<Task>> = flow {
        throw RuntimeException("No se pudieron cargar las tareas")
    }

    override suspend fun insertTask(task: Task) {
        // No-op
    }

    override suspend fun deleteTask(task: Task) {
        // No-op
    }
}

@RunWith(AndroidJUnit4::class)
class ScreenTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockAuthRepository(): AuthRepository {
        val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        every { mockFirebaseAuth.currentUser } returns mockk(relaxed = true) {
            every { uid } returns "test-user-id"
        }
        return AuthRepository(mockFirebaseAuth)
    }

    // --- MainScreen Tests ---

    @Test
    fun mainScreen_displaysTitle_button_andTaskList() {
        val tasks = listOf(
            Task(id = 1, name = "Tarea 1", description = "Desc 1"),
            Task(id = 2, name = "Tarea 2", description = "Desc 2")
        )
        val dao = FakeTaskDao(flow { emit(tasks) })
        val repository = TaskRepository(dao)
        val viewModel = TaskViewModel(repository, createMockAuthRepository())

        composeTestRule.setContent {
            MainScreen(onNavigateToForm = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("TodoAPP").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Nueva Tarea").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tarea 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tarea 2").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysEmptyStateMessage_whenNoTasks() {
        val dao = FakeTaskDao(flow { emit(emptyList()) })
        val repository = TaskRepository(dao)
        val viewModel = TaskViewModel(repository, createMockAuthRepository())

        composeTestRule.setContent {
            MainScreen(onNavigateToForm = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No hay tareas creadas aún").assertIsDisplayed()
    }

    @Test
    fun mainScreen_displaysErrorState_withRetryButton_onLoadFailure() {
        val dao = ErrorTaskDao()
        val repository = TaskRepository(dao)
        val viewModel = TaskViewModel(repository, createMockAuthRepository())

        composeTestRule.setContent {
            MainScreen(onNavigateToForm = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("No se pudieron cargar las tareas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    // --- FormScreen Tests ---

    @Test
    fun formScreen_displaysEmptyFields_onInitialDisplay() {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)
        val locationService = LocationService(mockk(relaxed = true))
        val viewModel = FormViewModel(repository, locationService, createMockAuthRepository())

        composeTestRule.setContent {
            FormScreen(onNavigateBack = {}, viewModel = viewModel)
        }

        // Labels for the text fields should be displayed
        composeTestRule.onNodeWithText("Nombre de la tarea").assertIsDisplayed()
        composeTestRule.onNodeWithText("Descripción").assertIsDisplayed()
    }

    @Test
    fun formScreen_displaysGuardarAndVolverButtons() {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)
        val locationService = LocationService(mockk(relaxed = true))
        val viewModel = FormViewModel(repository, locationService, createMockAuthRepository())

        composeTestRule.setContent {
            FormScreen(onNavigateBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Guardar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volver").assertIsDisplayed()
    }

    @Test
    fun formScreen_enforcesMaxCharacterLimit_onNameField() {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)
        val locationService = LocationService(mockk(relaxed = true))
        val viewModel = FormViewModel(repository, locationService, createMockAuthRepository())

        composeTestRule.setContent {
            FormScreen(onNavigateBack = {}, viewModel = viewModel)
        }

        // Type a string longer than 100 characters into the name field
        val longName = "A".repeat(150)
        composeTestRule.onNodeWithText("Nombre de la tarea", useUnmergedTree = true)
            .performTextInput(longName)

        // The ViewModel should truncate to 100 characters
        val truncatedName = "A".repeat(100)
        composeTestRule.onNodeWithText(truncatedName).assertIsDisplayed()
    }

    @Test
    fun formScreen_enforcesMaxCharacterLimit_onDescriptionField() {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)
        val locationService = LocationService(mockk(relaxed = true))
        val viewModel = FormViewModel(repository, locationService, createMockAuthRepository())

        composeTestRule.setContent {
            FormScreen(onNavigateBack = {}, viewModel = viewModel)
        }

        // Type a string longer than 500 characters into the description field
        val longDescription = "B".repeat(600)
        composeTestRule.onNodeWithText("Descripción", useUnmergedTree = true)
            .performTextInput(longDescription)

        // The ViewModel should truncate to 500 characters
        val truncatedDescription = "B".repeat(500)
        composeTestRule.onNodeWithText(truncatedDescription).assertIsDisplayed()
    }
}
