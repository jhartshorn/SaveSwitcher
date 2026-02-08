package com.saveswitcher.ui.model

data class EmulatorUiModel(
    val id: String,
    val name: String,
    val folderUri: String,
    val extensions: List<String>,
)

data class UserUiModel(
    val id: String,
    val displayName: String,
    val normalizedId: String,
)

data class SaveFileUiModel(
    val uri: String,
    val filename: String,
    val modifiedAt: Long,
    val modifiedAtLabel: String,
)

data class GameUiModel(
    val id: String,
    val emulatorId: String,
    val emulatorName: String,
    val relativeDir: String,
    val baseName: String,
    val extension: String,
    val directoryUri: String,
    val displayName: String,
    val baseSave: SaveFileUiModel?,
    val userSaves: Map<String, SaveFileUiModel>,
)
