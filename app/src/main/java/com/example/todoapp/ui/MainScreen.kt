package com.example.todoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todoapp.data.Task
import com.example.todoapp.viewmodel.TaskListUiState
import com.example.todoapp.viewmodel.TaskViewModel

/**
 * Main screen composable that displays the task list, app title, and navigation controls.
 *
 * Renders the "TodoAPP" heading, an "Agregar Nueva Tarea" button for navigating to the
 * task creation form, a "Mapa" button for navigating to the map screen, and a list of
 * saved tasks. Handles loading, empty, error, and populated states. Integrates a
 * [DeleteConfirmationDialog] for confirming task deletions, a "Cerrar Sesión" button
 * for logging out, and a Snackbar for displaying transient error/logout messages.
 *
 * @param onNavigateToForm Callback invoked when the user presses the "Agregar Nueva Tarea"
 *   button, triggering navigation to the form screen.
 * @param onNavigateToMap Callback invoked when the user presses the "Mapa" button,
 *   triggering navigation to the map screen.
 * @param onLogout Callback invoked when the user presses the "Cerrar Sesión" button.
 *   If logout fails, the caller should trigger a snackbar message via the viewModel.
 * @param viewModel The [TaskViewModel] providing the task list UI state and delete operations.
 */
@Composable
fun MainScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onLogout: () -> Unit = {},
    onEditTask: (Task) -> Unit = {},
    viewModel: TaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarEvent by viewModel.snackbarEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarEvent) {
        snackbarEvent?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarEvent()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TodoAPP",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )

                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Cerrar Sesión",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToForm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Agregar Nueva Tarea",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is TaskListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TaskListUiState.Success -> {
                    if (state.tasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay tareas creadas aún",
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.tasks, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onDeleteClick = { taskToDelete = it },
                                    onEditClick = { onEditTask(it) }
                                )
                            }
                        }
                    }
                }

                is TaskListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.retryLoad() }) {
                                Text(
                                    text = "Reintentar",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            task = task,
            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Deseas cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Sí, cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
