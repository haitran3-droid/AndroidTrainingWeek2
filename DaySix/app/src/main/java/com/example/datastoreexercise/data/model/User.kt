package com.example.datastoreexercise.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho bảng "users" trong cơ sở dữ liệu Room SQLite.
 *
 * @property indices Tạo chỉ mục cho cột 'email' với ràng buộc 'unique = true' để đảm bảo
 * mỗi email chỉ tồn tại duy nhất một lần trong database.
 */
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    // Khóa chính tự động tăng (autoGenerate = true)
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    // Ánh xạ thuộc tính fullName vào cột "full_name" trong bảng SQLite
    @ColumnInfo(name = "full_name")
    val fullName: String,

    // Ánh xạ thuộc tính email vào cột "email" trong bảng SQLite
    @ColumnInfo(name = "email")
    val email: String
)