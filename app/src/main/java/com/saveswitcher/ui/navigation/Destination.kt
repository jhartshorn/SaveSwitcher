package com.saveswitcher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Emulators("emulators", "Emulators", Icons.Outlined.Storage),
    Games("games", "Games", Icons.Outlined.SportsEsports),
    Users("users", "Users", Icons.Outlined.Group),
    History("history", "History", Icons.Outlined.History),
}

data class NavItem(val destination: Destination)

val bottomNavItems = Destination.entries.map { NavItem(it) }
