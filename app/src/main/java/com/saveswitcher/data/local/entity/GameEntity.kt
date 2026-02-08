package com.saveswitcher.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = EmulatorEntity::class,
            parentColumns = ["id"],
            childColumns = ["emulator_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["emulator_id", "relative_dir", "base_name", "extension"], unique = true)],
)
data class GameEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "emulator_id") val emulatorId: String,
    @ColumnInfo(name = "relative_dir") val relativeDir: String,
    @ColumnInfo(name = "base_name") val baseName: String,
    val extension: String,
    @ColumnInfo(name = "last_seen_at") val lastSeenAt: Long,
)
