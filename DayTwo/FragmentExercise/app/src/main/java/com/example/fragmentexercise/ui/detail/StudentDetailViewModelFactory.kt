package com.example.fragmentexercise.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fragmentexercise.data.repository.StudentRepository

class StudentDetailViewModelFactory(
    private val repository: StudentRepository
) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}