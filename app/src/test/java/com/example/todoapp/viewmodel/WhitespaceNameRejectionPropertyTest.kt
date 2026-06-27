package com.example.todoapp.viewmodel

import com.example.todoapp.data.LocationService
import com.example.todoapp.data.Task
import com.example.todoapp.data.TaskDao
import com.example.todoapp.repository.TaskRepository
import com.google.android.gms.location.FusedLocationProviderClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Property-based test verifying that whitespace-only names are rejected by FormViewModel.
 *
 * **Validates: Requirements 3.4**
 *
 * Property 2: Whitespace-only names are rejected
 * For any string composed entirely of whitespace characters (spaces, tabs, newlines, or empty string),
 * attempting to save a Task with that string as the name should be rejected by the FormViewModel
 * validation, and the repository state should remain unchanged.
 */
class WhitespaceNameRejectionPropertyTest : FunSpec({

    test("Property 2: Whitespace-only names are rejected - saveTask returns false and repository remains empty") {
        /**
         * Validates: Requirements 3.4
         */
        checkAll(20, arbWhitespaceOnlyString()) { whitespaceInput ->
            val fakeDao = WhitespaceFakeTaskDao()
            val repository = TaskRepository(fakeDao)
            val locationService = LocationService(mockk<FusedLocationProviderClient>(relaxed = true))
            val mockFirebaseAuth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
            io.mockk.every { mockFirebaseAuth.currentUser } returns mockk(relaxed = true) {
                io.mockk.every { uid } returns "test-user-id"
            }
            val authRepo = com.example.todoapp.data.AuthRepository(mockFirebaseAuth)
            val viewModel = FormViewModel(repository, locationService, authRepo)

            // Set the name to the whitespace-only string
            viewModel.updateName(whitespaceInput)

            // Attempt to save
            val result = viewModel.saveTask()

            // Verify: save is rejected
            result shouldBe false

            // Verify: nameError is set to the expected message
            viewModel.uiState.value.nameError shouldBe "El nombre no puede estar vacío"

            // Verify: no tasks were inserted into the DAO
            fakeDao.getAllTasks().first().size shouldBe 0
        }
    }
})

/**
 * Generates arbitrary whitespace-only strings of various lengths (0–50 characters).
 * Uses a mix of spaces, tabs, and newlines.
 */
private fun arbWhitespaceOnlyString(): Arb<String> = arbitrary {
    val whitespaceChars = listOf(' ', '\t', '\n', '\r')
    val length = Arb.int(0..50).bind()
    if (length == 0) {
        ""
    } else {
        buildString {
            repeat(length) {
                append(Arb.element(whitespaceChars).bind())
            }
        }
    }
}

/**
 * A fake in-memory implementation of [TaskDao] for testing whitespace name rejection.
 * Simulates Room's behavior: auto-generates IDs, stores tasks in insertion order,
 * and returns them ordered by ID ascending.
 */
private class WhitespaceFakeTaskDao : TaskDao {

    private var nextId = 1
    private val tasks = mutableListOf<Task>()
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    override fun getAllTasks(): Flow<List<Task>> = tasksFlow

    override fun getTasksForUser(userId: String): Flow<List<Task>> = tasksFlow

    override fun getTasksWithLocationForUser(userId: String): Flow<List<Task>> = tasksFlow

    override suspend fun insertTask(task: Task) {
        val taskWithId = if (task.id == 0) {
            task.copy(id = nextId++)
        } else {
            tasks.removeAll { it.id == task.id }
            task
        }
        tasks.add(taskWithId)
        tasksFlow.update { tasks.sortedBy { it.id }.toList() }
    }

    override suspend fun deleteTask(task: Task) {
        tasks.removeAll { it.id == task.id }
        tasksFlow.update { tasks.sortedBy { it.id }.toList() }
    }
}
