package com.example.fragmentexercise.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmentexercise.data.model.Student
import com.example.fragmentexercise.data.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class BaseViewModel(
    protected val repository: StudentRepository
) : ViewModel(){
    private val _student = MutableLiveData<Student?>()
    val student: LiveData<Student?> = _student

    fun getStudent(studentId: String){
        _student.value = null
        viewModelScope.launch(Dispatchers.IO){
            val student = repository.getStudentById(studentId)
            _student.postValue(student)
        }
    }
}