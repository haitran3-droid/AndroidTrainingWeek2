package com.example.datastoreexercise.factory

/**
 * Định nghĩa các loại hành động điều hướng (Navigation action) trong ứng dụng.
 * Được sử dụng bởi IntentFactory để tạo ra Intent tương ứng.
 */
enum class IntentActionType {
    CREATE,      // Mở màn hình tạo người dùng mới
    EDIT,        // Mở màn hình cập nhật thông tin người dùng
    VIEW_DETAIL  // Mở màn hình xem chi tiết người dùng
}