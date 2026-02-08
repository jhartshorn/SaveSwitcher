package com.saveswitcher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saveswitcher.data.local.entity.SwitchOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwitchOpDao {
    @Query("SELECT * FROM switch_ops ORDER BY started_at DESC")
    fun observeAll(): Flow<List<SwitchOpEntity>>

    @Query("SELECT * FROM switch_ops WHERE game_id = :gameId ORDER BY started_at DESC")
    fun observeByGame(gameId: String): Flow<List<SwitchOpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(operation: SwitchOpEntity)
}
