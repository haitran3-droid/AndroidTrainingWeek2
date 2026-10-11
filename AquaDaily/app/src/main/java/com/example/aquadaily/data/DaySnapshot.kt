package com.example.aquadaily.data

data class DaySnapshot(
    val dayEpoch: Long,
    val goalMl: Int,
    val entries: List<DrinkEntry>
) {
    val totalMl: Long
        get() = entries.sumOf { it.amountMl.toLong() }
}
