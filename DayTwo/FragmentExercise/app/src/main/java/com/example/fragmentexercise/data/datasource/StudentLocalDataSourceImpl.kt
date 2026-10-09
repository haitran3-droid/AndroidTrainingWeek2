package com.example.fragmentexercise.data.datasource

import com.example.fragmentexercise.data.datasource.file.StudentFileProvider
import com.example.fragmentexercise.data.model.Student
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class StudentLocalDataSourceImpl(
    private val studentFileProvider: StudentFileProvider,
    private val gson: Gson = Gson()
) : StudentLocalDataSource {
    override suspend fun addStudent(student: Student): Boolean {
        val students = getStudents().toMutableList()
        students.add(student)
        return saveStudents(students)
    }

    override suspend fun getStudents(): List<Student> {
        return try {
            val inputStream = studentFileProvider.openFileToRead(FILE_NAME)
            inputStream.bufferedReader().use { reader ->
                gson.fromJson(reader, object : TypeToken<List<Student>>() {}.type)
            } ?: emptyList()
        } catch (_: FileNotFoundException) {
            emptyList()
        }
    }

    override suspend fun getStudentById(id: String): Student? {
        val students = getStudents()
        return students.find{it.id == id}
    }

    override suspend fun updateStudent(student: Student): Boolean {
        val students = getStudents().toMutableList()
        val index = students.indexOfFirst { it.id == student.id }
        if(index != -1){
            students[index] = student
            return saveStudents(students)
        }
        return false
    }

    override suspend fun deleteStudent(studentId: String): Boolean {
        val students = getStudents()
        val remainingStudents = students.filter { it.id != studentId }
        if(remainingStudents.size == students.size){
            return false
        }
        return saveStudents(remainingStudents)
    }

    override suspend fun isStudentExist(id: String): Boolean {
        val students = getStudents()
        return students.any{it.id == id}
    }

    private suspend fun saveStudents(students: List<Student>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(students)
                studentFileProvider.openFileToWrite(FILE_NAME).use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }

    companion object {
        const val FILE_NAME = "students.json"
    }
}