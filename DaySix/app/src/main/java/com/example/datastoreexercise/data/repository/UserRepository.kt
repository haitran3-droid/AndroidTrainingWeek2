package com.example.datastoreexercise.data.repository

import com.example.datastoreexercise.data.dao.UserDao
import com.example.datastoreexercise.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Lớp triển khai cụ thể của interface Repository.
 * Nhận UserDao thông qua Constructor Injection và điều hướng các cuộc gọi dữ liệu tới DAO.
 */
class UserRepository(private val userDao: UserDao) : Repository {
    // Lấy toàn bộ người dùng từ DAO dưới dạng Flow
    override val users: Flow<List<User>> = userDao.getAll()

    // Thêm người dùng mới vào database qua DAO
    override suspend fun save(user: User): Long {
        return userDao.insert(user)
    }

    // Lấy luồng dữ liệu của 1 người dùng theo ID
    override fun getUserById(id: Int): Flow<User?> {
        return userDao.findById(id)
    }

    // Xóa người dùng qua DAO
    override suspend fun deleteUser(user: User): Int {
        return userDao.delete(user)
    }

    // Cập nhật người dùng qua DAO
    override suspend fun updateUser(user: User): Int {
        return userDao.update(user)
    }

    // Tìm kiếm người dùng có tên chứa chuỗi 'name' (sử dụng wildcard '%' cho LIKE query)
    override fun findByName(name: String): Flow<List<User>> {
        return userDao.findByName("%$name%")
    }
}
