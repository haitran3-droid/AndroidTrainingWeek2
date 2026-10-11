package com.example.aquadaily.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyGoal::class, DrinkEntry::class],
    version = 1,
    exportSchema = true
)
abstract class HydrationDatabase : RoomDatabase() {
    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var instance: HydrationDatabase? = null
        fun get(context: Context): HydrationDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                HydrationDatabase::class.java,
                "aqua_daily.db"
            ).build().also { instance = it }
        }
    }
}