package com.example.aquadaily

import android.app.Application
import com.example.aquadaily.data.HydrationDatabase
import com.example.aquadaily.data.HydrationRepository

open class AquaDailyApp : Application() {
    open val repository by lazy { HydrationRepository(HydrationDatabase.get(this)) }
}