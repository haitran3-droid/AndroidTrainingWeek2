package com.example.viewpager2exercise1.data.datasource

import com.example.viewpager2exercise1.data.model.Zodiac

interface ZodiacLocalDataSource {
    fun getZodiacs(): List<Zodiac>
}