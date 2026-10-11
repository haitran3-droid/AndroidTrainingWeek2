package com.example.viewpager2exercise1.data.repository

import com.example.viewpager2exercise1.data.datasource.ZodiacLocalDataSource
import com.example.viewpager2exercise1.data.model.Zodiac

class ZodiacRepositoryImpl(
    private val localDataSource: ZodiacLocalDataSource
) : ZodiacRepository{
    override fun getZodiacs(): List<Zodiac> {
        return localDataSource.getZodiacs()
    }
}