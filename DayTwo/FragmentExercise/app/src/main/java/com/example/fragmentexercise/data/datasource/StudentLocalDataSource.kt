package com.example.fragmentexercise.data.datasource

import com.example.fragmentexercise.data.model.Student

interface StudentLocalDataSource {
    suspend fun addStudent(student: Student): Boolean

    suspend fun getStudents(): List<Student>

    suspend fun getStudentById(id: String): Student?

    suspend fun updateStudent(student: Student): Boolean

    suspend fun deleteStudent(studentId: String): Boolean

    suspend fun isStudentExist(id: String): Boolean
}