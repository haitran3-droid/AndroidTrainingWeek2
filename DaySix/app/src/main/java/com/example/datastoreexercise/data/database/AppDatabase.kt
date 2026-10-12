package com.example.datastoreexercise.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.datastoreexercise.data.dao.UserDao
import com.example.datastoreexercise.data.model.User
/**
 * Cơ sở dữ liệu chính của ứng dụng sử dụng Room Database.
 *
 * @param entities Khai báo các Entity (bảng) trong database, ở đây có User::class.
 * @param version Phiên bản database, dùng để quản lý migration khi có thay đổi cấu trúc bảng.
 */
@Database(
    entities = [User::class], version = 1
)
abstract class AppDatabase : RoomDatabase() {
    // Cung cấp DAO để truy xuất bảng User
    abstract fun getUserDao(): UserDao

    companion object {
        /**
         * @Volatile đảm bảo mọi luồng (threads) đều đọc được giá trị mới nhất của INSTANCE ngay khi được ghi,
         * tránh tình trạng cache biến cục bộ trên từng CPU core.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Lấy thể hiện (instance) duy nhất của AppDatabase theo mẫu thiết kế Singleton.
         * Sử dụng synchronized block (Double-checked locking) để đảm bảo Thread-safe.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database" // Tên file cơ sở dữ liệu SQLite trong bộ nhớ trong
                ).fallbackToDestructiveMigration() // Nếu thay đổi version mà không viết Migration, Room sẽ tự xóa DB cũ và tạo mới
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
