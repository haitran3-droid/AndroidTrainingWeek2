package com.example.fragmentexercise.app

import android.app.Application
import com.example.fragmentexercise.data.datasource.StudentLocalDataSourceImpl
import com.example.fragmentexercise.data.datasource.file.StudentFileProviderImpl
import com.example.fragmentexercise.data.model.Gender
import com.example.fragmentexercise.data.model.serialization.GenderDeserializer
import com.example.fragmentexercise.data.model.serialization.GenderSerializer
import com.example.fragmentexercise.data.repository.StudentRepository
import com.example.fragmentexercise.data.repository.StudentRepositoryImpl
import com.google.gson.GsonBuilder

class StudentApp : Application() {
    lateinit var studentRepository: StudentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val fileProvider = StudentFileProviderImpl(this)
        val gson = GsonBuilder()
            .setDateFormat("dd/MM/yyyy")
            .registerTypeAdapter(Gender::class.java, GenderDeserializer())
            .registerTypeAdapter(Gender::class.java, GenderSerializer())
            .create()
        val dataSource = StudentLocalDataSourceImpl(fileProvider, gson)
        studentRepository = StudentRepositoryImpl(dataSource)
    }
}