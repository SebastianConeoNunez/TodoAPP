package com.example.todoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.LocationService
import com.example.todoapp.data.Task
import com.example.todoapp.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state representation for the task creation form screen.
 *
 * @param name The current value of the task name input field.
 * @param description The current value of the task description input field.
 * @param nameError An optional validation error message for the name field, or null if valid.
 * @param saveError An optional error message when persistence fails, or null if no error.
 * @param isSaving Whether a save operation is currently in progress.
 * @param latitude Optional latitude coordinate associated with the task.
 * @param longitude Optional longitude coordinate associated with the task.
 * @param isLoadingLocation Whether a location fetch operation is currently in progress.
 * @param locationError An optional error message when fetching location fails, or null if no error.
 */
data class FormUiState(
    val name: String = "",
    val description: String = "",
    val nameError: String? = null,
    val saveError: String? = null,
    val isSaving: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null
)

/**
 * ViewModel that manages the state and business logic for the task creation form.
 *
 * Handles input validation, character limit enforcement, location association,
 * and task persistence through the [TaskRepository]. Exposes a [StateFlow] of
 * [FormUiState] for the UI to observe reactively.
 *
 * @param repository The [TaskRepository] used to persist new tasks to local storage.
 * @param locationService The [LocationService] used to obtain the device's current GPS coordinates.
 * @param userId The identifier of the currently authenticated user, used to associate tasks with their owner.
 */
class FormViewModel(
    private val repository: TaskRepository,
    private val locationService: LocationService,
    private val authRepository: com.example.todoapp.data.AuthRepository
) : ViewModel() {

    private val userId: String get() = authRepository.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(FormUiState())

    /**
     * The current UI state of the form screen as an observable [StateFlow].
     */
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    /**
     * Updates the task name field value, enforcing a maximum length of 100 characters.
     *
     * If the provided name exceeds 100 characters, only the first 100 characters are kept.
     * Any existing name validation error is cleared when the user modifies the name.
     *
     * @param name The new name value entered by the user.
     */
    fun updateName(name: String) {
        _uiState.update { currentState ->
            currentState.copy(
                name = name.take(MAX_NAME_LENGTH),
                nameError = null
            )
        }
    }

    /**
     * Updates the task description field value, enforcing a maximum length of 500 characters.
     *
     * If the provided description exceeds 500 characters, only the first 500 characters are kept.
     *
     * @param description The new description value entered by the user.
     */
    fun updateDescription(description: String) {
        _uiState.update { currentState ->
            currentState.copy(
                description = description.take(MAX_DESCRIPTION_LENGTH)
            )
        }
    }

    /**
     * Obtains the current device location and associates it with the task being created.
     *
     * Uses [LocationService.getCurrentLocation] with a 10-second timeout.
     * On success, updates the UI state with the obtained latitude and longitude.
     * On failure, sets a location error message in Spanish.
     */
    fun addLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }

            val result = locationService.getCurrentLocation(timeoutMs = 10_000)

            result.fold(
                onSuccess = { latLng ->
                    _uiState.update {
                        it.copy(
                            latitude = latLng.latitude,
                            longitude = latLng.longitude,
                            isLoadingLocation = false,
                            locationError = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            locationError = LOCATION_ERROR
                        )
                    }
                }
            )
        }
    }

    /**
     * Removes the location associated with the task being created.
     *
     * Clears latitude, longitude, and any location error from the UI state.
     */
    fun removeLocation() {
        _uiState.update {
            it.copy(
                latitude = null,
                longitude = null,
                locationError = null
            )
        }
    }

    /**
     * Sets a specific location for the task (used when the user taps the map or searches an address).
     *
     * @param latitude The latitude of the selected location.
     * @param longitude The longitude of the selected location.
     */
    fun setLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                locationError = null
            )
        }
    }

    /**
     * Validates the form input and persists the task to local storage.
     *
     * Validation rules:
     * - The task name must not be blank or consist only of whitespace characters.
     *
     * The task is saved with the current user's [userId] and any associated location coordinates.
     *
     * If validation fails, sets [FormUiState.nameError] and returns false.
     * If persistence fails, sets [FormUiState.saveError] and returns false.
     * On success, returns true so the UI can trigger navigation back.
     *
     * @return true if the task was saved successfully, false otherwise.
     */
    suspend fun saveTask(): Boolean {
        val currentState = _uiState.value

        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(nameError = NAME_BLANK_ERROR) }
            return false
        }

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        return try {
            val task = Task(
                id = editingTaskId ?: 0,
                name = currentState.name.trim(),
                description = currentState.description.trim(),
                latitude = currentState.latitude,
                longitude = currentState.longitude,
                userId = userId
            )
            repository.insertTask(task)
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(saveError = SAVE_ERROR) }
            false
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    /**
     * Resets the form state to its initial empty values.
     */
    fun resetForm() {
        _uiState.value = FormUiState()
        editingTaskId = null
    }

    /** The id of the task being edited, or null if creating a new task. */
    private var editingTaskId: Int? = null

    /**
     * Loads an existing task into the form for editing.
     *
     * @param task The task to edit.
     */
    fun loadTask(task: com.example.todoapp.data.Task) {
        editingTaskId = task.id
        _uiState.value = FormUiState(
            name = task.name,
            description = task.description,
            latitude = task.latitude,
            longitude = task.longitude
        )
    }

    companion object {
        /** Maximum allowed length for the task name field. */
        const val MAX_NAME_LENGTH = 100

        /** Maximum allowed length for the task description field. */
        const val MAX_DESCRIPTION_LENGTH = 500

        /** Error message displayed when the task name is blank or whitespace-only. */
        const val NAME_BLANK_ERROR = "El nombre no puede estar vacío"

        /** Error message displayed when persisting the task fails. */
        const val SAVE_ERROR = "No se pudo guardar la tarea"

        /** Error message displayed when the location service fails or times out. */
        const val LOCATION_ERROR = "No se pudo obtener la ubicación"
    }
}
