package com.saveswitcher.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saveswitcher.ui.model.EmulatorUiModel

private data class EditState(
    val id: String?,
    val name: String,
    val extensions: String,
    val folderUri: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorListScreen(
    emulators: List<EmulatorUiModel>,
    onAddEmulator: (name: String, folderUri: String, extensions: List<String>) -> Unit,
    onUpdateEmulator: (id: String, name: String, folderUri: String, extensions: List<String>) -> Unit,
    onDeleteEmulator: (id: String) -> Unit,
) {
    val context = LocalContext.current
    var editState by remember {
        mutableStateOf(
            EditState(
                id = null,
                name = "",
                extensions = ".sav,.dsv,.srm",
                folderUri = null,
            ),
        )
    }
    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<EmulatorUiModel?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            editState = editState.copy(folderUri = uri.toString())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Emulators", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Register each emulator and its save folder.",
                style = MaterialTheme.typography.bodyLarge,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (emulators.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No emulators added yet. Use + to add one.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                items(emulators, key = { it.id }) { emulator ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = emulator.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = emulator.folderUri,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Extensions: ${emulator.extensions.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        editState = EditState(
                                            id = emulator.id,
                                            name = emulator.name,
                                            extensions = emulator.extensions.joinToString(","),
                                            folderUri = emulator.folderUri,
                                        )
                                        showSheet = true
                                    },
                                ) {
                                    Text("Edit")
                                }
                                TextButton(onClick = { pendingDelete = emulator }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editState = EditState(
                    id = null,
                    name = "",
                    extensions = ".sav,.dsv,.srm",
                    folderUri = null,
                )
                showSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "Add emulator")
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (editState.id == null) "Add Emulator" else "Edit Emulator",
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = editState.name,
                    onValueChange = { editState = editState.copy(name = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Emulator name") },
                )

                OutlinedTextField(
                    value = editState.extensions,
                    onValueChange = { editState = editState.copy(extensions = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Save extensions") },
                )

                Text(
                    text = editState.folderUri ?: "No folder selected",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )

                TextButton(onClick = { folderPicker.launch(null) }) {
                    Text("Pick Save Folder")
                }

                TextButton(
                    onClick = {
                        val folder = editState.folderUri ?: return@TextButton
                        val extensions = editState.extensions
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (editState.name.isBlank() || extensions.isEmpty()) return@TextButton

                        if (editState.id == null) {
                            onAddEmulator(editState.name.trim(), folder, extensions)
                        } else {
                            onUpdateEmulator(editState.id!!, editState.name.trim(), folder, extensions)
                        }
                        showSheet = false
                    },
                    enabled = editState.name.isNotBlank() && editState.folderUri != null,
                ) {
                    Text(if (editState.id == null) "Save Emulator" else "Update Emulator")
                }

                TextButton(onClick = { showSheet = false }) {
                    Text("Cancel")
                }
            }
        }
    }

    pendingDelete?.let { emulator ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove Emulator") },
            text = {
                Text("Delete profile \"${emulator.name}\"? Save files on disk will not be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEmulator(emulator.id)
                        pendingDelete = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
