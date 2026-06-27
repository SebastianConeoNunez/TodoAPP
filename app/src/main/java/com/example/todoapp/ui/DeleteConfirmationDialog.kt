package com.example.todoapp.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.todoapp.data.Task

/**
 * A confirmation dialog displayed when the user requests to delete a task.
 *
 * Shows the task name in the confirmation message and provides "Confirmar"
 * and "Cancelar" action buttons.
 *
 * @param task The [Task] that the user intends to delete.
 * @param onConfirm Callback invoked when the user confirms the deletion.
 * @param onDismiss Callback invoked when the user cancels the deletion or dismisses the dialog.
 */
@Composable
fun DeleteConfirmationDialog(
    task: Task,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Eliminar tarea")
        },
        text = {
            Text(text = "¿Estás seguro de que deseas eliminar la tarea \"${task.name}\"?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
