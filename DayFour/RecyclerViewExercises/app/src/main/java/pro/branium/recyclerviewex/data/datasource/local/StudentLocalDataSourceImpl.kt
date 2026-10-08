package pro.branium.recyclerviewex.data.datasource.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.branium.recyclerviewex.data.model.Student
import java.io.FileNotFoundException

class StudentLocalDataSourceImpl(
    private val fileProvider: FileProvider,
    private val gson: Gson = Gson()
) : StudentLocalDataSource {
    override suspend fun addStudent(student: Student): Boolean {
        val students = getStudents().toMutableList()
        students.add(student)
        return saveStudents(students)
    }

    override suspend fun getStudents(): List<Student> {
        return try {
            val inputStream = fileProvider.openFileToRead(FILE_NAME)
            inputStream.bufferedReader().use { reader ->
                gson.fromJson(
                    reader,
                    object : TypeToken<List<Student>>() {}.type
                ) ?: emptyList()
            }
        } catch (_: FileNotFoundException) {
            emptyList()
        }
    }

    override suspend fun getStudentById(id: String): Student? {
        val students = getStudents()
        return students.find { it.id == id }
    }

    override suspend fun updateStudent(student: Student): Boolean {
        val students = getStudents().toMutableList()
        val index = students.indexOfFirst { it.id == student.id }
        if (index != -1) {
            students[index] = student
            return saveStudents(students)
        }
        return false
    }

    override suspend fun deleteStudent(student: Student): Boolean {
        val students = getStudents().toMutableList()
        val index = students.indexOfFirst{it.id == student.id}
        if(index != -1){
            students.removeAt(index)
            return saveStudents(students)
        }
        return false
    }

    private suspend fun saveStudents(students: List<Student>): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val outputStream = fileProvider.openFileToWrite(FILE_NAME)
                val json = gson.toJson(students)
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                outputStream.close()
                true
            } catch (_: Exception) {
                false
            }
        }

    override suspend fun isStudentExist(id: String): Boolean {
        val students = getStudents()
        return students.any { it.id == id }
    }

    companion object {
        const val FILE_NAME = "students.json"
    }
}