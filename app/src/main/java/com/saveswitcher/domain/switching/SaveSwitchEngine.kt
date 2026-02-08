package com.saveswitcher.domain.switching

import com.saveswitcher.domain.model.GameSaveDescriptor
import com.saveswitcher.domain.model.SwitchPreview
import com.saveswitcher.domain.model.SwitchResult

interface SaveSwitchEngine {
    suspend fun preview(
        game: GameSaveDescriptor,
        targetOwnerId: String,
    ): SwitchPreview

    suspend fun switchUser(
        game: GameSaveDescriptor,
        sourceOwnerId: String?,
        targetOwnerId: String,
    ): SwitchResult
}
