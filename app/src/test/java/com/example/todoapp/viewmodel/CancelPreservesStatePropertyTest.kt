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
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Property-based test verifying that cancelling task creation preserves repository state.
 *
 * **Validates: Requirements 4.2, 4.4**
 *
 * Property 3: Cancel preserves repository state
 * For any sequence of form inputs (arbitrary name and description strings), if the user
 * cancels without pressing "Guardar," the repository's task list should remain identical
 * to its state before navigation to the Form Screen.
 */
class CancelPreservesStatePropertyTest : FunSpec({

    test("Property 3: Cancel preserves repository state - form inputs without save do not modify repository") {
        /**
         * Validates: Requirements 4.2, 4.4
         */
        checkAll(20, Arb.string(0..200), Arb.string(0..600)) { arbitraryName, arbitraryDescription ->
            // Set up a fake DAO with some pre-populated tasks
            val fakeDao = FakeTaskDao()
            val repository = TaskRepository(fakeDao)

            // Pre-populate with some tasks
            repository.insertTask(Task(name = "Existing Task 1", description = "Description 1"))
            repository.insertTask(Task(name = "Existing Task 2", description = "Description 2"))
            repository.insertTask(Task(name = "Existing Task 3", description = ""))

            // Snapshot the repository state before the "cancel" flow
            val tasksBefore = fakeDao.getAllTasks().first()

            // Simulate navigating to FormScreen and entering arbitrary input
            val locationService = LocationService(mockk<FusedLocationProviderClient>(relaxed = true))
            val mockFirebaseAuth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
            io.mockk.every { mockFirebaseAuth.currentUser } returns mockk(relaxed = true) {
                io.mockk.every { uid } returns "test-user-id"
            }
            val authRepo = com.example.todoapp.data.AuthRepository(mockFirebaseAuth)
            val formViewModel = FormViewModel(repository, locationService, authRepo)
            formViewModel.updateName(arbitraryName)
            formViewModel.updateDescription(arbitraryDescription)

            // Simulate cancel: do NOT call saveTask() — just discard the ViewModel

            // Verify the repository's task list is identical after the cancel flow
            val tasksAfter = fakeDao.getAllTasks().first()
            tasksAfter shouldBe tasksBefore
        }
    }
})

/**
 * A fake in-memory implementation of [TaskDao] for testing purposes.
 * Simulates Room's behavior: auto-generates IDs, stores tasks in insertion order,
 * and returns them ordered by ID ascending.
 */
private class FakeTaskDao : TaskDao {

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
            // OnConflictStrategy.REPLACE: remove existing with same id
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
