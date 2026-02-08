package com.saveswitcher.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "switch_ops")
data class SwitchOpEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "game_id") val gameId: String,
    @ColumnInfo(name = "emulator_id") val emulatorId: String,
    @ColumnInfo(name = "source_owner_id") val sourceOwnerId: String?,
    @ColumnInfo(name = "target_owner_id") val targetOwnerId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    val status: String,
    @ColumnInfo(name = "details_json") val detailsJson: String,
)
