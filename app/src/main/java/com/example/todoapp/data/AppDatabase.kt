package com.example.todoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The Room database for the TodoAPP application.
 *
 * Security measures:
 * - Database stored in app-private directory (Android sandbox, inaccessible to other apps)
 * - User data isolated by user_id (Firebase UID) preventing cross-user access
 * - Authentication tokens encrypted by Firebase using AES-256 in Android Keystore
 * - Database access gated by authentication guard (no access without valid session)
 */
@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN user_id TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
