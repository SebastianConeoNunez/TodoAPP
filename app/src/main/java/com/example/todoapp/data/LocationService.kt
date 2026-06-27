package com.example.todoapp.data

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Wrapper around FusedLocationProviderClient that exposes location as
 * suspend functions and Flows for use in ViewModels and UI layer.
 *
 * Permission checks are expected to happen at the UI layer before calling
 * these methods. SecurityException is caught and returned as Result.failure.
 */
class LocationService(private val fusedLocationClient: FusedLocationProviderClient) {

    /**
     * Gets the current device location with a configurable timeout.
     *
     * @param timeoutMs Maximum time in milliseconds to wait for a location fix. Defaults to 10 seconds.
     * @return Result.success with LatLng if location obtained, Result.failure otherwise.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(timeoutMs: Long = 10_000): Result<LatLng> {
        return try {
            val location = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(LatLng(loc.latitude, loc.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación dentro del tiempo límite"))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    /**
     * Provides continuous location updates as a Flow.
     *
     * @param intervalMs Interval in milliseconds between location updates. Defaults to 5 seconds.
     * @return Flow emitting LatLng values as the device location changes.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 5_000): Flow<LatLng> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LatLng(location.latitude, location.longitude))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
