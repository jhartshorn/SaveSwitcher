package com.saveswitcher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saveswitcher.data.local.entity.GameStateEntity

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE game_id = :gameId")
    suspend fun getByGameId(gameId: String): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(gameState: GameStateEntity)
}
