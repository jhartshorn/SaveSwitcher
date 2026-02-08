package com.saveswitcher.domain.model

data class SwitchPreview(
    val baseExists: Boolean,
    val sourceOwnerKnown: Boolean,
    val sourceOwnerId: String?,
    val sourceModifiedAt: Long?,
    val targetOwnerId: String,
    val targetVariantExists: Boolean,
    val targetModifiedAt: Long?,
)
