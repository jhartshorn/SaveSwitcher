package com.saveswitcher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.saveswitcher.ui.screens.EmulatorListScreen
import com.saveswitcher.ui.screens.GameListScreen
import com.saveswitcher.ui.screens.HistoryScreen
import com.saveswitcher.ui.screens.UserListScreen
import com.saveswitcher.ui.model.EmulatorUiModel
import com.saveswitcher.ui.model.GameUiModel
import com.saveswitcher.ui.model.UserUiModel

@Composable
fun SaveSwitcherNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    emulators: List<EmulatorUiModel>,
    users: List<UserUiModel>,
    games: List<GameUiModel>,
    knownOwnerByGame: Map<String, String>,
    historyEntries: List<String>,
    gameStatusMessage: String,
    isScanningGames: Boolean,
    onAddEmulator: (name: String, folderUri: String, extensions: List<String>) -> Unit,
    onUpdateEmulator: (id: String, name: String, folderUri: String, extensions: List<String>) -> Unit,
    onDeleteEmulator: (id: String) -> Unit,
    onAddUser: (displayName: String) -> Unit,
    onScanGames: () -> Unit,
    onSwitchUser: (game: GameUiModel, targetUserId: String, sourceOwnerUserId: String?) -> Unit,
    onExportSave: (game: GameUiModel, exportFolderUri: String) -> Unit,
    onImportSave: (game: GameUiModel, importFileUri: String) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Emulators.route,
        modifier = modifier,
    ) {
        composable(Destination.Emulators.route) {
            EmulatorListScreen(
                emulators = emulators,
                onAddEmulator = onAddEmulator,
                onUpdateEmulator = onUpdateEmulator,
                onDeleteEmulator = onDeleteEmulator,
            )
        }
        composable(Destination.Games.route) {
            GameListScreen(
                games = games,
                users = users,
                knownOwnerByGame = knownOwnerByGame,
                statusMessage = gameStatusMessage,
                isScanning = isScanningGames,
                onScanGames = onScanGames,
                onSwitchUser = onSwitchUser,
                onExportSave = onExportSave,
                onImportSave = onImportSave,
            )
        }
        composable(Destination.Users.route) {
            UserListScreen(
                users = users,
                onAddUser = onAddUser,
            )
        }
        composable(Destination.History.route) {
            HistoryScreen(historyEntries = historyEntries)
        }
    }
}
