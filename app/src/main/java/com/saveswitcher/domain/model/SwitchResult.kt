package com.saveswitcher.domain.model

data class SwitchResult(
    val success: Boolean,
    val operationId: String,
    val message: String,
)
