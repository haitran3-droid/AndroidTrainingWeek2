package com.example.fragmentexercise.data.repository

import com.example.fragmentexercise.data.datasource.StudentLocalDataSource
import com.example.fragmentexercise.data.model.Student

class StudentRepositoryImpl(
    private val dataSource: StudentLocalDataSource
) : StudentRepository {
    override suspend fun addStudent(student: Student): Boolean {
        return dataSource.addStudent(student)
    }

    override suspend fun getStudents(): List<Student> {
        return dataSource.getStudents()
    }

    override suspend fun getStudentById(id: String): Student? {
        return dataSource.getStudentById(id)
    }

    override suspend fun updateStudent(student: Student): Boolean {
        return dataSource.updateStudent(student)
    }

    override suspend fun deleteStudent(studentId: String): Boolean {
        return dataSource.deleteStudent(studentId)
    }

    override suspend fun isStudentExist(id: String): Boolean {
        return dataSource.isStudentExist(id)
    }
}