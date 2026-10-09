package com.example.fragmentexercise.ui.studentform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fragmentexercise.data.repository.StudentRepository

class StudentFormViewModelFactory(
    val repository: StudentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentFormViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentFormViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}