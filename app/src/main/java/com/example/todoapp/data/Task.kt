package com.example.todoapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a to-do task persisted in the local Room database.
 *
 * @param id The unique auto-generated identifier for the task.
 * @param name The name of the task (1–100 characters, non-blank).
 * @param description The description of the task (0–500 characters, may be empty).
 * @param latitude Optional latitude coordinate for the task's associated location.
 * @param longitude Optional longitude coordinate for the task's associated location.
 * @param userId The identifier of the user who owns this task.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "user_id") val userId: String = ""
)
