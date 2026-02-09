package com.saveswitcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.saveswitcher.data.local.SaveSwitcherDatabaseProvider
import com.saveswitcher.data.local.entity.EmulatorEntity
import com.saveswitcher.data.local.entity.SwitchOpEntity
import com.saveswitcher.data.local.entity.UserEntity
import com.saveswitcher.domain.saf.SaveFileService
import com.saveswitcher.ui.model.EmulatorUiModel
import com.saveswitcher.ui.model.GameUiModel
import com.saveswitcher.ui.model.UserUiModel
import com.saveswitcher.ui.navigation.SaveSwitcherNavHost
import com.saveswitcher.ui.navigation.SaveSwitcherTopLevelDestination
import com.saveswitcher.ui.navigation.bottomNavItems
import com.saveswitcher.ui.theme.SaveSwitcherTheme
import com.saveswitcher.util.UserIdNormalizer
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaveSwitcherTheme {
                SaveSwitcherApp()
            }
        }
    }
}

@Composable
private fun SaveSwitcherApp() {
    val context = LocalContext.current
    val db = remember(context) { SaveSwitcherDatabaseProvider.get(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val saveFileService = remember(context) { SaveFileService(context) }

    val emulatorEntities by db.emulatorDao().observeAll().collectAsState(initial = emptyList())
    val userEntities by db.userDao().observeActiveUsers().collectAsState(initial = emptyList())
    val switchOps by db.switchOpDao().observeAll().collectAsState(initial = emptyList())

    val emulators = remember(emulatorEntities) { emulatorEntities.map { it.toUiModel() } }
    val users = remember(userEntities) { userEntities.map { it.toUiModel() } }

    val games = remember { mutableStateListOf<GameUiModel>() }
    var gameStatusMessage by remember { mutableStateOf("Tap \"Scan Saves\" after adding emulators.") }
    var isScanningGames by remember { mutableStateOf(false) }

    val knownOwnerByGame = remember(switchOps) {
        buildMap {
            switchOps
                .sortedByDescending { it.startedAt }
                .forEach { op ->
                    if (!contains(op.gameId)) {
                        put(op.gameId, op.targetOwnerId)
                    }
                }
        }
    }

    val historyEntries = remember(switchOps) {
        switchOps
            .sortedByDescending { it.startedAt }
            .map { op ->
                "${DATE_FORMATTER.format(Instant.ofEpochMilli(op.startedAt))} • ${op.gameId.substringBefore('|')} • ${op.sourceOwnerId ?: "Unknown"} -> ${op.targetOwnerId} • ${op.status}"
            }
    }

    LaunchedEffect(emulators) {
        if (emulators.isEmpty()) {
            games.clear()
            return@LaunchedEffect
        }

        isScanningGames = true
        val scanned = saveFileService.scanGames(emulators)
        games.clear()
        games.addAll(orderGamesByRecentSwitch(scanned, switchOps))
        isScanningGames = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            SaveSwitcherTopLevelDestination(
                navController = navController,
                items = bottomNavItems,
            )
        },
    ) { paddingValues ->
        SaveSwitcherNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            emulators = emulators,
            users = users,
            games = games,
            knownOwnerByGame = knownOwnerByGame,
            historyEntries = historyEntries,
            gameStatusMessage = gameStatusMessage,
            isScanningGames = isScanningGames,
            onAddEmulator = { name, folderUri, extensions ->
                val cleanedExtensions = extensions
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val duplicate = emulators.any { it.name == name && it.folderUri == folderUri }
                if (duplicate || cleanedExtensions.isEmpty()) {
                    return@SaveSwitcherNavHost
                }

                scope.launch {
                    val now = System.currentTimeMillis()
                    db.emulatorDao().upsert(
                        EmulatorEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            treeUri = folderUri,
                            extensionsJson = cleanedExtensions.joinToString(","),
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            },
            onUpdateEmulator = { id, name, folderUri, extensions ->
                val cleanedExtensions = extensions
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (name.isBlank() || folderUri.isBlank() || cleanedExtensions.isEmpty()) {
                    return@SaveSwitcherNavHost
                }

                val existing = emulatorEntities.firstOrNull { it.id == id } ?: return@SaveSwitcherNavHost
                scope.launch {
                    val now = System.currentTimeMillis()
                    db.emulatorDao().upsert(
                        existing.copy(
                            name = name,
                            treeUri = folderUri,
                            extensionsJson = cleanedExtensions.joinToString(","),
                            updatedAt = now,
                        ),
                    )
                }
            },
            onDeleteEmulator = { id ->
                scope.launch {
                    db.emulatorDao().deleteById(id)
                    games.removeAll { it.emulatorId == id }
                }
            },
            onAddUser = { displayName ->
                val normalizedId = UserIdNormalizer.normalize(displayName)
                if (normalizedId.isBlank()) {
                    return@SaveSwitcherNavHost
                }
                val alreadyExists = users.any { it.normalizedId == normalizedId }
                if (alreadyExists) {
                    return@SaveSwitcherNavHost
                }

                scope.launch {
                    val now = System.currentTimeMillis()
                    db.userDao().upsert(
                        UserEntity(
                            id = UUID.randomUUID().toString(),
                            displayName = displayName,
                            normalizedId = normalizedId,
                            isArchived = false,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            },
            onScanGames = {
                scope.launch {
                    if (emulators.isEmpty()) {
                        gameStatusMessage = "Add at least one emulator first."
                        return@launch
                    }
                    isScanningGames = true
                    gameStatusMessage = "Scanning save folders..."
                    val scanned = saveFileService.scanGames(emulators)
                    games.clear()
                    games.addAll(orderGamesByRecentSwitch(scanned, switchOps))
                    gameStatusMessage = "Found ${scanned.size} game save group(s)."
                    isScanningGames = false
                }
            },
            onSwitchUser = { game, targetUserId, sourceOwnerUserId ->
                scope.launch {
                    val message = saveFileService.switchUser(game, targetUserId, sourceOwnerUserId)
                    val now = System.currentTimeMillis()
                    val status = if (message.startsWith("Switched") || message.startsWith("No save")) {
                        "success"
                    } else {
                        "error"
                    }

                    val op = SwitchOpEntity(
                        id = UUID.randomUUID().toString(),
                        gameId = game.id,
                        emulatorId = game.emulatorId,
                        sourceOwnerId = sourceOwnerUserId,
                        targetOwnerId = targetUserId,
                        startedAt = now,
                        endedAt = now,
                        status = status,
                        detailsJson = "{\"message\":\"${message.replace("\"", "'")}\"}",
                    )
                    db.switchOpDao().upsert(op)

                    gameStatusMessage = message
                    isScanningGames = true
                    val rescanned = saveFileService.scanGames(emulators)
                    games.clear()
                    games.addAll(orderGamesByRecentSwitch(rescanned, switchOps + op))
                    isScanningGames = false
                }
            },
            onExportSave = { game, exportFolderUri ->
                scope.launch {
                    val message = saveFileService.exportCurrentSave(game, exportFolderUri)
                    gameStatusMessage = message
                }
            },
            onImportSave = { game, importFileUri ->
                scope.launch {
                    val message = saveFileService.importSave(game, importFileUri)
                    gameStatusMessage = message

                    isScanningGames = true
                    val rescanned = saveFileService.scanGames(emulators)
                    games.clear()
                    games.addAll(orderGamesByRecentSwitch(rescanned, switchOps))
                    isScanningGames = false
                }
            },
        )
    }
}

private fun EmulatorEntity.toUiModel(): EmulatorUiModel {
    return EmulatorUiModel(
        id = id,
        name = name,
        folderUri = treeUri,
        extensions = extensionsJson
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() },
    )
}

private fun UserEntity.toUiModel(): UserUiModel {
    return UserUiModel(
        id = id,
        displayName = displayName,
        normalizedId = normalizedId,
    )
}

private fun orderGamesByRecentSwitch(
    games: List<GameUiModel>,
    switchOps: List<SwitchOpEntity>,
): List<GameUiModel> {
    val latestSwitchByGame = switchOps
        .groupBy { it.gameId }
        .mapValues { (_, ops) -> ops.maxOfOrNull { it.startedAt } ?: 0L }

    return games.sortedWith(
        compareByDescending<GameUiModel> { latestSwitchByGame[it.id] ?: 0L }
            .thenByDescending { it.baseSave?.modifiedAt ?: 0L }
            .thenBy { it.displayName.lowercase() },
    )
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
