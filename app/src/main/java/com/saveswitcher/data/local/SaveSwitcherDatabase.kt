package com.saveswitcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.saveswitcher.data.local.dao.EmulatorDao
import com.saveswitcher.data.local.dao.GameDao
import com.saveswitcher.data.local.dao.GameStateDao
import com.saveswitcher.data.local.dao.SwitchOpDao
import com.saveswitcher.data.local.dao.UserDao
import com.saveswitcher.data.local.entity.EmulatorEntity
import com.saveswitcher.data.local.entity.GameEntity
import com.saveswitcher.data.local.entity.GameStateEntity
import com.saveswitcher.data.local.entity.SwitchOpEntity
import com.saveswitcher.data.local.entity.UserEntity

@Database(
    entities = [
        EmulatorEntity::class,
        UserEntity::class,
        GameEntity::class,
        GameStateEntity::class,
        SwitchOpEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SaveSwitcherDatabase : RoomDatabase() {
    abstract fun emulatorDao(): EmulatorDao
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun gameStateDao(): GameStateDao
    abstract fun switchOpDao(): SwitchOpDao
}
