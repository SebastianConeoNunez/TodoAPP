package com.example.todoapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for performing database operations on [Task] entities.
 *
 * Provides methods to query, insert, and delete tasks from the local Room database.
 */
@Dao
interface TaskDao {

    /**
     * Retrieves all tasks from the database ordered by their insertion order.
     *
     * @return A [Flow] emitting the list of all tasks, ordered by id ascending.
     */
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Inserts a new task into the database. If a task with the same primary key
     * already exists, it will be replaced.
     *
     * @param task The [Task] entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    /**
     * Deletes the specified task from the database.
     *
     * @param task The [Task] entity to delete.
     */
    @Delete
    suspend fun deleteTask(task: Task)

    /**
     * Retrieves all tasks belonging to a specific user, ordered by id ascending.
     *
     * @param userId The identifier of the user whose tasks to retrieve.
     * @return A [Flow] emitting the list of tasks for the given user.
     */
    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY id ASC")
    fun getTasksForUser(userId: String): Flow<List<Task>>

    /**
     * Retrieves all tasks belonging to a specific user that have location data
     * (non-null latitude and longitude), ordered by id ascending.
     *
     * @param userId The identifier of the user whose geolocated tasks to retrieve.
     * @return A [Flow] emitting the list of geolocated tasks for the given user.
     */
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY id ASC")
    fun getTasksWithLocationForUser(userId: String): Flow<List<Task>>
}
