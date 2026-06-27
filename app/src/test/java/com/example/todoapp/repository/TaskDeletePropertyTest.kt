package com.example.todoapp.repository

import com.example.todoapp.data.Task
import com.example.todoapp.data.TaskDao
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Property-based test verifying that deleting a task removes it permanently.
 *
 * **Validates: Requirements 5.2, 5.3, 6.3**
 *
 * Property 4: Delete removes task permanently
 * For any persisted Task, confirming its deletion via the TaskRepository should result in
 * subsequent queries returning a list that does not contain that Task, and the list length
 * should decrease by exactly one.
 */
class TaskDeletePropertyTest : FunSpec({

    test("Property 4: Delete removes task permanently - deleted task no longer appears and list size decreases by 1") {
        /**
         * Validates: Requirements 5.2, 5.3, 6.3
         */
        checkAll(20, arbTaskList()) { taskNames ->
            // Need at least 1 task to delete
            if (taskNames.isEmpty()) return@checkAll

            val fakeDao = FakeTaskDao()
            val repository = TaskRepository(fakeDao)

            // Insert all tasks
            taskNames.forEach { (name, description) ->
                repository.insertTask(Task(name = name, description = description))
            }

            // Get current state
            val tasksBeforeDelete = fakeDao.getAllTasks().first()
            val sizeBeforeDelete = tasksBeforeDelete.size

            // Pick a random task to delete (use first for determinism within this iteration)
            val taskToDelete = tasksBeforeDelete.random()

            // Delete the task
            repository.deleteTask(taskToDelete)

            // Verify: task no longer appears in subsequent queries
            val tasksAfterDelete = fakeDao.getAllTasks().first()
            tasksAfterDelete shouldNotContain taskToDelete

            // Verify: list size decreased by exactly 1
            tasksAfterDelete.size shouldBe (sizeBeforeDelete - 1)

            // Verify: the task ID is gone from the results
            tasksAfterDelete.none { it.id == taskToDelete.id } shouldBe true
        }
    }
})

/**
 * Generates a list of 1–10 task name/description pairs for property testing.
 * Each name is 1–100 non-blank characters, each description is 0–500 characters.
 */
private fun arbTaskList(): Arb<List<Pair<String, String>>> = arbitrary {
    val count = Arb.int(1..10).bind()
    (1..count).map {
        val name = arbValidTaskName().bind()
        val description = arbValidDescription().bind()
        Pair(name, description)
    }
}

/**
 * Generates arbitrary valid task names: 1–100 characters, non-blank (at least one non-whitespace character).
 */
private fun arbValidTaskName(): Arb<String> = arbitrary {
    val length = Arb.int(1..100).bind()
    val base = Arb.string(length..length).bind()
    // Ensure at least one non-whitespace character
    if (base.isBlank()) {
        base.replaceFirst(base.first().toString(), "a")
    } else {
        base
    }
}

/**
 * Generates arbitrary valid descriptions: 0–500 characters (may be empty).
 */
private fun arbValidDescription(): Arb<String> = arbitrary {
    val length = Arb.int(0..500).bind()
    if (length == 0) "" else Arb.string(length..length).bind()
}

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
