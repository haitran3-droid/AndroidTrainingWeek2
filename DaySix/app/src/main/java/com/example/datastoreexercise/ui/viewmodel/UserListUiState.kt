package com.example.datastoreexercise.ui.viewmodel

import com.example.datastoreexercise.data.model.User
/**
 * Trạng thái giao diện (UI State) đại diện cho danh sách người dùng trên MainActivity.
 * Áp dụng mô hình Unidirectional Data Flow (UDF).
 *
 * @property users Danh sách người dùng hiện tại (tất cả hoặc theo kết quả tìm kiếm).
 * @property query Từ khóa tìm kiếm hiện tại.
 * @property isLoading Trạng thái đang tải dữ liệu.
 */
data class UserListUiState(
    val users: List<User> = emptyList(),
    val query: String ="",
    val isLoading: Boolean = true
)
