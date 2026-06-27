package com.example.todoapp.repository

import com.example.todoapp.data.Task
import com.example.todoapp.data.TaskDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides a clean API for accessing task data.
 *
 * Acts as a single source of truth by wrapping [TaskDao] operations.
 * Exceptions from the underlying DAO are not caught here and propagate
 * to the caller (typically a ViewModel) for appropriate error handling.
 *
 * @param taskDao The Data Access Object used to perform database operations on tasks.
 */
class TaskRepository(private val taskDao: TaskDao) {

    /**
     * Retrieves all tasks as a reactive stream.
     *
     * The returned [Flow] emits a new list whenever the underlying data changes,
     * enabling the UI to stay up-to-date automatically.
     *
     * @return A [Flow] emitting the current list of all [Task] entities ordered by id ascending.
     */
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    /**
     * Inserts a new task into the database.
     *
     * If a task with the same primary key already exists, it will be replaced.
     *
     * @param task The [Task] entity to insert.
     */
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    /**
     * Deletes the specified task from the database.
     *
     * @param task The [Task] entity to delete.
     */
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    /**
     * Retrieves all tasks belonging to a specific user as a reactive stream.
     *
     * @param userId The identifier of the user whose tasks to retrieve.
     * @return A [Flow] emitting the list of tasks for the given user, ordered by id ascending.
     */
    fun getTasksForUser(userId: String): Flow<List<Task>> = taskDao.getTasksForUser(userId)

    /**
     * Retrieves all tasks belonging to a specific user that have location data
     * (non-null latitude and longitude) as a reactive stream.
     *
     * @param userId The identifier of the user whose geolocated tasks to retrieve.
     * @return A [Flow] emitting the list of geolocated tasks for the given user, ordered by id ascending.
     */
    fun getTasksWithLocationForUser(userId: String): Flow<List<Task>> = taskDao.getTasksWithLocationForUser(userId)
}
