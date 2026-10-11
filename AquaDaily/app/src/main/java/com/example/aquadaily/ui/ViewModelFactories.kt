package com.example.aquadaily.ui

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aquadaily.data.HydrationRepository
import com.example.aquadaily.ui.home.HomeViewModel
import com.example.aquadaily.ui.report.ReportViewModel

/**
 * Khởi tạo [androidx.lifecycle.ViewModelProvider.Factory] cho [HomeViewModel],
 * cung cấp dependency [HydrationRepository].
 */
fun homeFactory(repository: HydrationRepository) =
    viewModelFactory { initializer { HomeViewModel(repository) } }

/**
 * Khởi tạo [androidx.lifecycle.ViewModelProvider.Factory] cho [ReportViewModel],
 * cung cấp dependency [HydrationRepository] cùng [androidx.lifecycle.SavedStateHandle] để lưu/khôi phục trạng thái.
 */
fun reportFactory(repository: HydrationRepository) = viewModelFactory {
    initializer { ReportViewModel(repository, createSavedStateHandle()) }
}