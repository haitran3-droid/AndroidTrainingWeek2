package pro.branium.recyclerviewex.data.repository

import android.text.style.UpdateLayout
import pro.branium.recyclerviewex.data.datasource.local.StudentLocalDataSource
import pro.branium.recyclerviewex.data.model.Student

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

    override suspend fun deleteStudent(student: Student): Boolean {
        return dataSource.deleteStudent(student)
    }

    override suspend fun isStudentExist(id: String): Boolean {
        return dataSource.isStudentExist(id)
    }
}