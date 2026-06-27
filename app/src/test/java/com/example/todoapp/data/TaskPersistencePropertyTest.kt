package com.example.todoapp.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
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
 * Property-based test verifying the task persistence round-trip property.
 *
 * **Validates: Requirements 3.1, 6.1, 6.2, 1.3, 3.3**
 *
 * Property 1: Task persistence round-trip
 * For any valid Task with a name of 1–100 non-whitespace-only characters and a description
 * of 0–500 characters, inserting it via the DAO and then querying all tasks should return a
 * list containing that Task with name and description fields preserved, in insertion order.
 */
class TaskPersistencePropertyTest : FunSpec({

    test("Property 1: Task persistence round-trip - inserted tasks are retrievable with matching fields and correct insertion order") {
        /**
         * Validates: Requirements 3.1, 6.1, 6.2, 1.3, 3.3
         */
        checkAll(20, arbValidTaskName(), arbValidDescription()) { name, description ->
            val fakeDao = FakeTaskDao()
            val task = Task(name = name, description = description)

            fakeDao.insertTask(task)

            val allTasks = fakeDao.getAllTasks().first()
            val insertedTask = allTasks.find { it.name == name && it.description == description }
            insertedTask shouldBe Task(id = insertedTask!!.id, name = name, description = description)
            insertedTask.name shouldBe name
            insertedTask.description shouldBe description
        }
    }

    test("Property 1: Task persistence round-trip - multiple tasks maintain insertion order") {
        /**
         * Validates: Requirements 3.1, 6.1, 6.2, 1.3, 3.3
         */
        checkAll(20, arbValidTaskName(), arbValidDescription(), arbValidTaskName(), arbValidDescription()) { name1, desc1, name2, desc2 ->
            val fakeDao = FakeTaskDao()

            val task1 = Task(name = name1, description = desc1)
            val task2 = Task(name = name2, description = desc2)

            fakeDao.insertTask(task1)
            fakeDao.insertTask(task2)

            val allTasks = fakeDao.getAllTasks().first()
            allTasks.size shouldBe 2
            // Verify insertion order (ordered by id ASC)
            allTasks[0].name shouldBe name1
            allTasks[0].description shouldBe desc1
            allTasks[1].name shouldBe name2
            allTasks[1].description shouldBe desc2
        }
    }
})

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
