package com.saveswitcher.domain.switching

import com.saveswitcher.domain.model.GameSaveDescriptor
import com.saveswitcher.domain.model.SwitchPreview
import com.saveswitcher.domain.model.SwitchResult
import java.util.UUID

class DefaultSaveSwitchEngine : SaveSwitchEngine {
    override suspend fun preview(
        game: GameSaveDescriptor,
        targetOwnerId: String,
    ): SwitchPreview {
        // Placeholder implementation. SAF-backed file probing will be added in milestone 4.
        return SwitchPreview(
            baseExists = false,
            sourceOwnerKnown = false,
            sourceOwnerId = null,
            sourceModifiedAt = null,
            targetOwnerId = targetOwnerId,
            targetVariantExists = false,
            targetModifiedAt = null,
        )
    }

    override suspend fun switchUser(
        game: GameSaveDescriptor,
        sourceOwnerId: String?,
        targetOwnerId: String,
    ): SwitchResult {
        // Planned algorithm:
        // 1) Validate access and free space.
        // 2) Backup base file to .bak.
        // 3) Archive base to source variant.
        // 4) Restore target variant to base (or leave absent for fresh save).
        // 5) Log operation and return result.
        return SwitchResult(
            success = false,
            operationId = UUID.randomUUID().toString(),
            message = "Switch engine is scaffolded but not implemented yet.",
        )
    }
}
