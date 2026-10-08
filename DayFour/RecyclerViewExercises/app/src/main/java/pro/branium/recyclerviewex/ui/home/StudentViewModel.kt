package pro.branium.recyclerviewex.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pro.branium.recyclerviewex.data.model.Student
import pro.branium.recyclerviewex.data.repository.StudentRepository

class StudentViewModel (
    private val repository: StudentRepository
): ViewModel(){
    private val _students = MutableLiveData<List<Student>>()
    val student: LiveData<List<Student>> = _students

    init {
        loadStudents()
    }

    fun loadStudents() {
        _students.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val students = repository.getStudents()
            _students.postValue(students)
        }
    }

    class Factory(private val repository: StudentRepository): ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StudentViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
