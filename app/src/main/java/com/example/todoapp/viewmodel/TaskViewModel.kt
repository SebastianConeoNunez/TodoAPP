package com.example.todoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.Task
import com.example.todoapp.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the task list UI state and task operations.
 *
 * Collects tasks from the [TaskRepository] as a reactive flow and exposes them
 * as a [StateFlow] of [TaskListUiState] for the UI to observe. Handles deletion
 * with error recovery, emitting appropriate UI states on failure.
 *
 * @param repository The [TaskRepository] used to access and modify task data.
 * @param userId The authenticated user's identifier used to filter tasks.
 */
class TaskViewModel(
    private val repository: TaskRepository,
    private val authRepository: com.example.todoapp.data.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskListUiState>(TaskListUiState.Loading)
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val _snackbarEvent = MutableStateFlow<String?>(null)
    val snackbarEvent: StateFlow<String?> = _snackbarEvent.asStateFlow()

    private var currentUserId: String = authRepository.currentUser?.uid ?: ""
    private var loadJob: Job? = null

    init {
        observeAuthAndLoadTasks()
    }

    private fun observeAuthAndLoadTasks() {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                val newUserId = user?.uid ?: ""
                if (newUserId != currentUserId || loadJob == null) {
                    currentUserId = newUserId
                    loadTasks()
                }
            }
        }
    }

    private fun loadTasks() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TaskListUiState.Loading
            if (currentUserId.isEmpty()) {
                _uiState.value = TaskListUiState.Success(emptyList())
                return@launch
            }
            repository.getTasksForUser(currentUserId)
                .catch { e ->
                    _uiState.value = TaskListUiState.Error(
                        e.message ?: "No se pudieron cargar las tareas"
                    )
                }
                .collect { tasks ->
                    _uiState.value = TaskListUiState.Success(tasks)
                }
        }
    }

    /**
     * Deletes the specified task from the repository.
     *
     * On success, the task list will automatically update via the collected flow.
     * On failure, the current list state is preserved and a snackbar event is emitted
     * with an error message.
     *
     * @param task The [Task] to delete.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Exception) {
                _snackbarEvent.value = e.message ?: "No se pudo eliminar la tarea"
            }
        }
    }

    /**
     * Re-initiates the task loading process.
     *
     * Useful when the initial load failed and the user wants to retry.
     * Resets the UI state to [TaskListUiState.Loading] and re-collects from
     * the repository flow.
     */
    fun retryLoad() {
        loadTasks()
    }

    /**
     * Clears the current snackbar event after it has been consumed by the UI.
     */
    fun clearSnackbarEvent() {
        _snackbarEvent.value = null
    }
}

/**
 * Represents the possible UI states for the task list screen.
 */
sealed class TaskListUiState {

    /**
     * Indicates that tasks are currently being loaded from the repository.
     */
    object Loading : TaskListUiState()

    /**
     * Indicates that tasks were successfully loaded.
     *
     * @param tasks The list of [Task] entities to display.
     */
    data class Success(val tasks: List<Task>) : TaskListUiState()

    /**
     * Indicates that an error occurred while loading tasks.
     *
     * @param message A human-readable error message describing what went wrong.
     */
    data class Error(val message: String) : TaskListUiState()
}
