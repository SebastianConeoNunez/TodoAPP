package com.example.todoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.LocationService
import com.example.todoapp.repository.TaskRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the map screen UI state.
 *
 * Handles the current device location via [LocationService] and exposes
 * task markers from [TaskRepository] for geolocated tasks belonging to
 * the authenticated user.
 *
 * @param taskRepository The repository used to query geolocated tasks.
 * @param locationService The service providing device location updates.
 * @param userId The identifier of the currently authenticated user.
 */
class MapViewModel(
    private val taskRepository: TaskRepository,
    private val locationService: LocationService,
    private val userId: String
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapUiState())

    /**
     * The current UI state for the map screen.
     *
     * Emits updates when the device location changes, task markers are loaded,
     * or the permission state is updated from the UI.
     */
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    private var locationUpdatesJob: Job? = null

    init {
        loadTaskMarkers()
    }

    /**
     * Loads tasks with location data for the authenticated user and transforms
     * them into [TaskMarker] objects for display on the map.
     */
    private fun loadTaskMarkers() {
        viewModelScope.launch {
            taskRepository.getTasksWithLocationForUser(userId)
                .catch { /* silently handle errors; markers remain empty */ }
                .collect { tasks ->
                    val markers = tasks.mapNotNull { task ->
                        val lat = task.latitude
                        val lng = task.longitude
                        if (lat != null && lng != null) {
                            TaskMarker(
                                taskId = task.id,
                                name = task.name,
                                description = task.description,
                                position = LatLng(lat, lng)
                            )
                        } else {
                            null
                        }
                    }
                    _mapState.value = _mapState.value.copy(taskMarkers = markers)
                }
        }
    }

    /**
     * Starts collecting continuous location updates from [LocationService].
     *
     * Each new location emitted by the service updates [mapState]'s currentLocation.
     * If updates are already active, this method is a no-op.
     */
    fun startLocationUpdates() {
        if (locationUpdatesJob?.isActive == true) return

        locationUpdatesJob = viewModelScope.launch {
            locationService.locationUpdates()
                .catch { /* silently handle location errors */ }
                .collect { latLng ->
                    _mapState.value = _mapState.value.copy(currentLocation = latLng)
                }
        }
    }

    /**
     * Stops the active location updates by cancelling the collection job.
     */
    fun stopLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
    }

    /**
     * Updates the location permission state from the UI layer.
     *
     * Called after the user responds to a permission request so the map screen
     * can reflect the current permission status.
     *
     * @param state The new [LocationPermissionState] reported by the UI.
     */
    fun updatePermissionState(state: LocationPermissionState) {
        _mapState.value = _mapState.value.copy(permissionState = state)
    }
}

/**
 * Represents the UI state for the map screen.
 *
 * @param currentLocation The device's current location, or null if not yet obtained.
 * @param taskMarkers List of task markers to display on the map.
 * @param permissionState The current location permission state.
 */
data class MapUiState(
    val currentLocation: LatLng? = null,
    val taskMarkers: List<TaskMarker> = emptyList(),
    val permissionState: LocationPermissionState = LocationPermissionState.NotRequested
)

/**
 * Represents a task with location data to be shown as a marker on the map.
 *
 * @param taskId The unique identifier of the task.
 * @param name The name of the task displayed on the marker.
 * @param description The description of the task shown in the marker info window.
 * @param position The geographic coordinates of the task.
 */
data class TaskMarker(
    val taskId: Int,
    val name: String,
    val description: String,
    val position: LatLng
)

/**
 * Represents the possible states of location permission as observed by the UI.
 */
enum class LocationPermissionState {
    /** Permission has not been requested yet. */
    NotRequested,
    /** Permission was granted by the user. */
    Granted,
    /** Permission was denied by the user (can be requested again). */
    Denied,
    /** Permission was permanently denied (user must go to settings). */
    PermanentlyDenied
}
