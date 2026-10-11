package com.example.viewpager2exercise1.data.repository

import com.example.viewpager2exercise1.data.model.Zodiac

interface ZodiacRepository {
    fun getZodiacs(): List<Zodiac>
}