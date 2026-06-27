package com.example.todoapp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.todoapp.viewmodel.LocationPermissionState
import com.example.todoapp.viewmodel.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Map screen that displays the user's current location and task markers on Google Maps.
 *
 * Handles location permission lifecycle:
 * - Requests ACCESS_FINE_LOCATION on first load if not already granted.
 * - On grant: starts location updates and shows the user's position marker.
 * - On deny: shows an informative message with a button to open app settings.
 * - On permanently denied: shows a message directing user to system settings.
 *
 * @param onNavigateBack Callback invoked when the user presses the back button.
 * @param mapViewModel The [MapViewModel] managing map UI state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val mapState by mapViewModel.mapState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            mapViewModel.updatePermissionState(LocationPermissionState.Granted)
            mapViewModel.startLocationUpdates()
        } else {
            // Check if we should show rationale — if not, it's permanently denied
            val activity = context.findActivity()
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } ?: false

            if (shouldShowRationale) {
                mapViewModel.updatePermissionState(LocationPermissionState.Denied)
            } else {
                mapViewModel.updatePermissionState(LocationPermissionState.PermanentlyDenied)
            }
        }
    }

    // Request permission on first load
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            mapViewModel.updatePermissionState(LocationPermissionState.Granted)
            mapViewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    mapState.currentLocation ?: LatLng(0.0, 0.0),
                    15f // ~500m radius
                )
            }

            // Update camera when first location is received
            LaunchedEffect(mapState.currentLocation) {
                mapState.currentLocation?.let { location ->
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Current location marker (blue)
                mapState.currentLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Mi ubicación",
                        snippet = "Posición actual"
                    )
                }

                // Task markers
                mapState.taskMarkers.forEach { taskMarker ->
                    Marker(
                        state = MarkerState(position = taskMarker.position),
                        title = taskMarker.name,
                        snippet = taskMarker.description
                    )
                }
            }

            // Permission denied overlay
            when (mapState.permissionState) {
                LocationPermissionState.Denied -> {
                    PermissionDeniedOverlay(
                        message = "Se necesita el permiso de ubicación para mostrar tu posición en el mapa",
                        onOpenSettings = { openAppSettings(context) }
                    )
                }

                LocationPermissionState.PermanentlyDenied -> {
                    PermissionDeniedOverlay(
                        message = "El permiso de ubicación fue denegado permanentemente. Habilítalo desde la configuración del dispositivo.",
                        onOpenSettings = { openAppSettings(context) }
                    )
                }

                else -> { /* No overlay needed */ }
            }
        }
    }
}

/**
 * Overlay displayed when location permission is denied or permanently denied.
 *
 * Shows an informative message and a button to open app settings.
 *
 * @param message The message to display to the user.
 * @param onOpenSettings Callback to open the application settings.
 */
@Composable
private fun PermissionDeniedOverlay(
    message: String,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Button(onClick = onOpenSettings) {
                Text("Abrir Configuración")
            }
        }
    }
}

/**
 * Opens the application details settings screen where the user can manage permissions.
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

/**
 * Utility extension to find the Activity from a Context.
 * Traverses the context wrapper chain to locate the hosting Activity.
 */
private fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
