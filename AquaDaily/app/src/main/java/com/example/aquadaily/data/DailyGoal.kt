package com.example.aquadaily.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey
    val dayEpoch: Long,
    val targetMl: Int
)
