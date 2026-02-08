package com.saveswitcher.domain.model

data class GameSaveDescriptor(
    val emulatorId: String,
    val relativeDir: String,
    val baseName: String,
    val extension: String,
)
