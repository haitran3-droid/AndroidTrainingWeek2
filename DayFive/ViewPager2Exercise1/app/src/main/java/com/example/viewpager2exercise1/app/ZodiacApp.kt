package com.example.viewpager2exercise1.app

import android.app.Application
import com.example.viewpager2exercise1.data.datasource.ZodiacLocalDataSourceImpl
import com.example.viewpager2exercise1.data.repository.ZodiacRepository
import com.example.viewpager2exercise1.data.repository.ZodiacRepositoryImpl

class ZodiacApp : Application() {
    private lateinit var repository: ZodiacRepository
    override fun onCreate() {
        super.onCreate()
        val localDataSource = ZodiacLocalDataSourceImpl()
        repository = ZodiacRepositoryImpl(localDataSource)
    }
    val getRepository : ZodiacRepository
        get() = repository
}