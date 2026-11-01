package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ModelMetadataEntity::class,
        ModelUsageLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PagePilotDatabase : RoomDatabase() {
    abstract fun pagePilotDao(): PagePilotDao

    companion object {
        @Volatile
        private var INSTANCE: PagePilotDatabase? = null

        fun getInstance(context: Context): PagePilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PagePilotDatabase::class.java,
                    "pagepilot_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
