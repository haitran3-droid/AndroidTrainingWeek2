package pro.branium.recyclerviewex.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.branium.recyclerviewex.data.model.Student
import pro.branium.recyclerviewex.data.repository.StudentRepository

open class BaseViewModel(protected val repository: StudentRepository) : ViewModel() {
    private val _student = MutableLiveData<Student?>()
    val student: LiveData<Student?> = _student

    fun getStudent(studentId: String){
        _student.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val student = repository.getStudentById(studentId)
            _student.postValue(student)
        }
    }
}