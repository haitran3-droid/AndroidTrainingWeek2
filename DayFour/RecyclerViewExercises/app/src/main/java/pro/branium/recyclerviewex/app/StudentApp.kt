package pro.branium.recyclerviewex.app

import android.app.Application
import com.google.gson.GsonBuilder
import pro.branium.recyclerviewex.data.datasource.local.FileProviderImpl
import pro.branium.recyclerviewex.data.datasource.local.StudentLocalDataSourceImpl
import pro.branium.recyclerviewex.data.model.Gender
import pro.branium.recyclerviewex.data.model.adapter.GenderDeserializer
import pro.branium.recyclerviewex.data.model.adapter.GenderSerializer
import pro.branium.recyclerviewex.data.repository.StudentRepository
import pro.branium.recyclerviewex.data.repository.StudentRepositoryImpl

class StudentApp : Application() {
    lateinit var studentRepository: StudentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val fileProvider = FileProviderImpl(this)
        val gson = GsonBuilder()
            .setDateFormat("dd/MM/yyyy")
            .registerTypeAdapter(Gender::class.java, GenderSerializer())
            .registerTypeAdapter(Gender::class.java, GenderDeserializer())
            .create()
        val dataSource = StudentLocalDataSourceImpl(fileProvider, gson)
        studentRepository = StudentRepositoryImpl(dataSource)
    }
}