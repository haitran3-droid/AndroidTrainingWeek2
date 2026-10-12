package com.example.datastoreexercise.ui

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.datastoreexercise.data.database.AppDatabase
import com.example.datastoreexercise.data.repository.UserRepository
import com.example.datastoreexercise.ui.viewmodel.UserViewModel
import com.example.datastoreexercise.ui.viewmodel.UserViewModelFactory

/**
 * Activity cơ sở (Base Class) cho các Activity sử dụng chung UserViewModel.
 * Tự động khởi tạo ViewModel kèm theo UserViewModelFactory, Database và UserRepository.
 */
abstract class BaseActivity : AppCompatActivity() {
    // Khởi tạo UserViewModel thông qua Kotlin property delegate `by viewModels`
    protected val viewModel: UserViewModel by viewModels {
        val database = AppDatabase.Companion.getDatabase(applicationContext)
        UserViewModelFactory(UserRepository(database.getUserDao()))
    }
}