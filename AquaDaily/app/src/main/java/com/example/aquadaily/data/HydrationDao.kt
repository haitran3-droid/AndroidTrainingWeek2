package com.example.aquadaily.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {
    /**
     * Lấy mục tiêu lượng nước của một ngày cụ thể.
     * @param day Ngày cần lấy mục tiêu (dạng epoch day).
     * @return Đối tượng [DailyGoal] tương ứng hoặc null nếu chưa được thiết lập.
     */
    @Query("SELECT * FROM daily_goals WHERE dayEpoch = :day")
    suspend fun goal(day: Long): DailyGoal?

    /**
     * Lấy mục tiêu mới nhất có mốc thời gian nhỏ hơn hoặc bằng ngày được chỉ định.
     * Thường dùng làm mục tiêu kế thừa cho ngày mới nếu ngày đó chưa được cài đặt mục tiêu riêng.
     */
    @Query("SELECT * FROM daily_goals WHERE dayEpoch <= :day ORDER BY dayEpoch DESC LIMIT 1")
    suspend fun latestGoal(day: Long): DailyGoal?

    /**
     * Quan sát mục tiêu lượng nước theo thời gian thực (Reactive) qua [Flow].
     * Tự động phát ra giá trị mới bất cứ khi nào mục tiêu của ngày này thay đổi trong database.
     */
    @Query("SELECT * FROM daily_goals WHERE dayEpoch <= :day ORDER BY dayEpoch DESC LIMIT 1")
    fun observeGoal(day: Long): Flow<DailyGoal?>

    /**
     * Lưu mục tiêu nước uống. Sử dụng `@Upsert` (Insert hoặc Update nếu khóa chính đã tồn tại).
     */
    @Upsert suspend fun saveGoal(goal: DailyGoal)

    /**
     * Thêm một bản ghi uống nước mới vào database.
     * @return ID của bản ghi vừa được tạo (do SQLite tự sinh).
     */
    @Insert suspend fun insertDrink(entry: DrinkEntry): Long

    /**
     * Xóa một bản ghi uống nước dựa theo ID.
     * @return Số lượng bản ghi bị xóa (1 nếu thành công, 0 nếu không tìm thấy).
     */
    @Query("DELETE FROM drink_entries WHERE id = :id") suspend fun deleteDrink(id: Long): Int

    /**
     * Lắng nghe danh sách các lần uống nước trong ngày theo thời gian thực [Flow].
     * Kết quả được sắp xếp giảm dần theo thời gian uống (`timestamp DESC, id DESC`) để lần uống mới nhất hiển thị lên đầu.
     */
    @Query("SELECT * FROM drink_entries WHERE dayEpoch = :day ORDER BY timestamp DESC, id DESC")
    fun observeDrinks(day: Long): Flow<List<DrinkEntry>>

    /**
     * Truy vấn danh sách các ngày đã hoàn thành chỉ tiêu uống nước (tổng ml uống được >= mục tiêu đề ra).
     * Sử dụng câu lệnh kết hợp bảng (INNER JOIN), nhóm theo ngày (GROUP BY)
     * và lọc qua mệnh đề HAVING để tính tổng (SUM).
     * Trả về dạng [Flow] để tự động cập nhật khi có thay đổi (dùng cho tính toán chuỗi ngày streak liên tục).
     */
    @Query("""
        SELECT d.dayEpoch FROM drink_entries d
        INNER JOIN daily_goals g ON d.dayEpoch = g.dayEpoch
        GROUP BY d.dayEpoch, g.targetMl
        HAVING SUM(d.amountMl) >= g.targetMl
    """)
    fun observeCompletedDays(): Flow<List<Long>>
}