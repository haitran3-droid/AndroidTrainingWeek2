package com.example.datastoreexercise.data.repository

import com.example.datastoreexercise.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Interface trừu tượng hóa tầng dữ liệu (Repository Pattern).
 * Đóng vai trò là nguồn dữ liệu duy nhất (Single Source of Truth) cho ViewModel,
 * giúp tách biệt hoàn toàn ViewModel với các chi tiết triển khai cụ thể của Room DAO.
 */
interface Repository {
    // Luồng dữ liệu danh sách người dùng được cập nhật tự động
    val users: Flow<List<User>>

    // Thêm người dùng mới, trả về rowId
    suspend fun save(user: User): Long

    // Lấy thông tin người dùng theo ID dạng Flow
    fun getUserById(id: Int): Flow<User?>

    // Xóa người dùng, trả về số dòng bị xóa
    suspend fun deleteUser(user: User): Int

    // Cập nhật người dùng, trả về số dòng được cập nhật
    suspend fun updateUser(user: User): Int

    // Tìm kiếm người dùng theo tên gần đúng dạng Flow
    fun findByName(name: String): Flow<List<User>>
}