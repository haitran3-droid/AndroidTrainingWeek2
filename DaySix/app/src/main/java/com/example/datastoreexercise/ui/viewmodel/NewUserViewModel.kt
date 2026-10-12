package com.example.datastoreexercise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datastoreexercise.data.model.User
import com.example.datastoreexercise.data.repository.Repository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý logic thêm mới người dùng trong NewUserActivity.
 */
class NewUserViewModel(private val repository: Repository) : ViewModel() {
    // Channel gửi sự kiện một lần (one-shot event) thông báo kết quả lưu thành công hay thất bại.
    // Channel.BUFFERED giúp đệm sự kiện trong trường hợp UI đang tạm dừng (ví dụ: xoay màn hình).
    private val saveResults = Channel<Boolean>(Channel.BUFFERED)
    val saveEvents: Flow<Boolean> = saveResults.receiveAsFlow()

    // StateFlow theo dõi trạng thái đang xử lý lưu để vô hiệu hóa (disable) nút bấm tránh click nhiều lần
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    /**
     * Lưu người dùng mới vào database thông qua Repository.
     * Kiểm tra trạng thái _isSaving để tránh việc gọi trùng lặp (debounce/prevent double click).
     */
    fun saveUser(fullName: String, email: String) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                // repository.save trả về rowId > 0 nếu chèn thành công, -1 nếu trùng email
                saveResults.send(operationSucceeded {
                    repository.save(User(0, fullName, email)) > 0
                })
            } finally {
                _isSaving.value = false
            }
        }
    }
}