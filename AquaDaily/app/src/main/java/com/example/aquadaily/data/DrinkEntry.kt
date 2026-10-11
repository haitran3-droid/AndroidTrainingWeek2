package com.example.aquadaily.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drink_entries",
    indices = [Index(value = ["dayEpoch", "timestamp"])]
)
data class DrinkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpoch: Long,
    val amountMl: Int,
    val timestamp: Long
)
