package com.saveswitcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saveswitcher.ui.model.GameUiModel
import com.saveswitcher.ui.model.UserUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    games: List<GameUiModel>,
    users: List<UserUiModel>,
    knownOwnerByGame: Map<String, String>,
    statusMessage: String,
    isScanning: Boolean,
    onScanGames: () -> Unit,
    onSwitchUser: (game: GameUiModel, targetUserId: String, sourceOwnerUserId: String?) -> Unit,
) {
    var selectedGame by remember { mutableStateOf<GameUiModel?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Games", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onScanGames) {
                Text("Scan Saves")
            }
            if (isScanning) {
                CircularProgressIndicator()
            }
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (games.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No game saves found yet. Add emulators, then tap Scan Saves.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            items(games, key = { it.id }) { game ->
                val owner = knownOwnerByGame[game.id]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = game.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Emulator: ${game.emulatorName}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (game.relativeDir.isBlank()) "Folder: /" else "Folder: /${game.relativeDir}",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Current owner: ${owner ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Current save: ${game.baseSave?.modifiedAtLabel ?: "No active save"}",
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        val variants = if (game.userSaves.isEmpty()) {
                            "No user saves"
                        } else {
                            game.userSaves.entries.joinToString(" | ") { "${it.key}: ${it.value.modifiedAtLabel}" }
                        }
                        Text(
                            text = variants,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        TextButton(
                            onClick = { selectedGame = game },
                            enabled = users.isNotEmpty(),
                        ) {
                            Text("Switch User")
                        }
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        val knownOwner = knownOwnerByGame[game.id]
        var sourceOwner by remember(game.id) { mutableStateOf(knownOwner) }
        var targetOwner by remember(game.id) { mutableStateOf(users.firstOrNull()?.normalizedId.orEmpty()) }

        ModalBottomSheet(onDismissRequest = { selectedGame = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Switch ${game.displayName}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Active save modified: ${game.baseSave?.modifiedAtLabel ?: "No active save"}",
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (game.baseSave != null && knownOwner == null) {
                    Text(text = "Who owns the current save?", style = MaterialTheme.typography.bodyLarge)
                    users.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = sourceOwner == user.normalizedId,
                                onClick = { sourceOwner = user.normalizedId },
                            )
                            Text(text = user.displayName)
                        }
                    }
                } else if (game.baseSave != null && knownOwner != null) {
                    Text(
                        text = "Current save owner: $knownOwner",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                Text(text = "Switch to user", style = MaterialTheme.typography.bodyLarge)
                users.forEach { user ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = targetOwner == user.normalizedId,
                            onClick = { targetOwner = user.normalizedId },
                        )
                        Text(text = user.displayName)
                    }
                }

                TextButton(
                    onClick = {
                        val effectiveSourceOwner = knownOwner ?: sourceOwner
                        onSwitchUser(game, targetOwner, effectiveSourceOwner)
                        selectedGame = null
                    },
                    enabled = targetOwner.isNotBlank() && (
                        game.baseSave == null || knownOwner != null || !sourceOwner.isNullOrBlank()
                    ),
                ) {
                    Text("Confirm Switch")
                }

                TextButton(onClick = { selectedGame = null }) {
                    Text("Cancel")
                }
            }
        }
    }
}
