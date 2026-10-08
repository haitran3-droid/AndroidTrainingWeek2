package pro.branium.recyclerviewex.data.datasource.local

import pro.branium.recyclerviewex.data.model.Student

interface StudentLocalDataSource {
    suspend fun addStudent(student: Student): Boolean

    suspend fun getStudents(): List<Student>

    suspend fun getStudentById(id: String): Student?

    suspend fun updateStudent(student: Student): Boolean

    suspend fun deleteStudent(student: Student) : Boolean

    suspend fun isStudentExist(id: String): Boolean
}