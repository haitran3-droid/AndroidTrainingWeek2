package com.example.datastoreexercise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.datastoreexercise.data.repository.Repository

/**
 * Factory class giúp khởi tạo các ViewModel có chứa tham số constructor (Repository).
 * Cần thiết vì cơ chế khởi tạo mặc định của Android ViewModelProvider không hỗ trợ constructor có tham số.
 */
class UserViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            UserViewModel::class.java -> UserViewModel(repository) as T
            NewUserViewModel::class.java -> NewUserViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}