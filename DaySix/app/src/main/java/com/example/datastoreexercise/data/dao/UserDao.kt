package com.example.datastoreexercise.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.datastoreexercise.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) cho bảng 'users'.
 * Định nghĩa các phương thức thao tác CRUD (Create, Read, Update, Delete) với cơ sở dữ liệu Room.
 */
@Dao
interface UserDao {
    /**
     * Lấy toàn bộ danh sách người dùng.
     * Trả về Flow<List<User>>: Room sẽ tự động phát ra (emit) danh sách mới mỗi khi bảng 'users' có thay đổi.
     */
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<User>>

    /**
     * Tìm người dùng theo ID cụ thể.
     * Trả về Flow<User?>: phát ra dữ liệu mới khi bản ghi có ID này được cập nhật.
     */
    @Query("SELECT * FROM users WHERE id = :id")
    fun findById(id: Int): Flow<User?>

    /**
     * Tìm kiếm người dùng có tên khớp với từ khóa (:name).
     * Thường kết hợp với toán tử LIKE '%keyword%' để tìm kiếm gần đúng.
     */
    @Query("SELECT * FROM users WHERE full_name LIKE :name")
    fun findByName(name: String): Flow<List<User>>

    /**
     * Thêm một người dùng mới vào cơ sở dữ liệu.
     * - onConflict = OnConflictStrategy.IGNORE: Nếu xảy ra trùng lặp (ví dụ email đã tồn tại do có ràng buộc unique),
     *   Room sẽ bỏ qua (không chèn) và không làm crash ứng dụng.
     * - Trả về: rowId của bản ghi được chèn (số dương nếu thành công, -1 nếu bị bỏ qua do trùng lặp).
     * - suspend: Chạy bất đồng bộ với Coroutines ngoài Main Thread.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long

    /**
     * Cập nhật thông tin của người dùng.
     * - onConflict = OnConflictStrategy.IGNORE: Bỏ qua nếu dữ liệu cập nhật gây xung đột ràng buộc.
     * - Trả về: số lượng dòng được cập nhật thành công trong SQLite (1 nếu thành công, 0 nếu thất bại).
     */
    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun update(user: User): Int

    /**
     * Xóa một người dùng khỏi cơ sở dữ liệu dựa trên Primary Key (@PrimaryKey id).
     * - Trả về: số lượng dòng bị xóa thành công trong SQLite (1 nếu xóa thành công).
     */
    @Delete
    suspend fun delete(user: User): Int
}