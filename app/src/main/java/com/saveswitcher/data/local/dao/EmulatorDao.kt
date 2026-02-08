package com.saveswitcher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.saveswitcher.data.local.entity.EmulatorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmulatorDao {
    @Query("SELECT * FROM emulators ORDER BY name ASC")
    fun observeAll(): Flow<List<EmulatorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(emulator: EmulatorEntity)

    @Query("DELETE FROM emulators WHERE id = :id")
    suspend fun deleteById(id: String)
}
