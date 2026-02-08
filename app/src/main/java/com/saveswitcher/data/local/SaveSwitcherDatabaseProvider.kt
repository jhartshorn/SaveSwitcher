package com.saveswitcher.data.local

import android.content.Context
import androidx.room.Room

object SaveSwitcherDatabaseProvider {
    @Volatile
    private var instance: SaveSwitcherDatabase? = null

    fun get(context: Context): SaveSwitcherDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SaveSwitcherDatabase::class.java,
                "save_switcher.db",
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
