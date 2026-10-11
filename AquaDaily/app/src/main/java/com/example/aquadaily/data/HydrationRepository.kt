package com.example.aquadaily.data

import androidx.room.withTransaction
import com.example.aquadaily.domain.HydrationMath
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.LocalDate

class HydrationRepository(
    private val db: HydrationDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val dao = db.hydrationDao()

    /**
     * Trả về epoch day của ngày hôm nay theo múi giờ của [clock].
     */
    fun today(): Long = LocalDate.now(clock).toEpochDay()

    /**
     * Lắng nghe trạng thái uống nước của một ngày cụ thể dưới dạng [Flow].
     * Kết hợp (combine) luồng mục tiêu ([HydrationDao.observeGoal]) và luồng danh sách uống ([HydrationDao.observeDrinks])
     * thành [DaySnapshot]. Sử dụng [distinctUntilChanged] để tránh phát lại nếu dữ liệu không thay đổi.
     */
    fun observeDay(day: Long) =
        combine(dao.observeGoal(day), dao.observeDrinks(day)) { goal, rows ->
            DaySnapshot(day, goal?.targetMl ?: HydrationMath.DEFAULT_GOAL, rows)
        }.distinctUntilChanged()

    /**
     * Lắng nghe danh sách tất cả các ngày đã hoàn thành mục tiêu (dùng để tính streak/chuỗi ngày).
     */
    fun observeCompletedDays() = dao.observeCompletedDays().distinctUntilChanged()

    /**
     * Đảm bảo một ngày có mục tiêu trong cơ sở dữ liệu.
     * Nếu ngày đó chưa có mục tiêu riêng, sẽ lấy mục tiêu của ngày gần nhất trước đó
     * hoặc dùng giá trị mặc định [HydrationMath.DEFAULT_GOAL].
     */
    private suspend fun ensureGoal(day: Long) {
        if (dao.goal(day) == null) {
            dao.saveGoal(
                DailyGoal(
                    day,
                    dao.latestGoal(day)?.targetMl ?: HydrationMath.DEFAULT_GOAL
                )
            )
        }
    }

    /**
     * Khởi tạo và đảm bảo ngày hôm nay có mục tiêu trong database.
     * Chạy bên trong Room Transaction để đảm bảo tính toàn vẹn dữ liệu.
     */
    suspend fun ensureToday() = db.withTransaction { ensureGoal(today()) }

    /**
     * Thêm một lần uống nước mới.
     *
     * @param amountMl Lượng nước uống (ml), yêu cầu nằm trong khoảng 1..5000ml.
     * Thao tác được bọc trong [withTransaction] để đảm bảo ngày hôm đó đã được tạo mục tiêu trước khi chèn bản ghi uống.
     */
    suspend fun addDrink(amountMl: Int) {
        require(amountMl in 1..5000)
        val instant = clock.instant()
        val day = instant.atZone(clock.zone).toLocalDate().toEpochDay()
        db.withTransaction {
            ensureGoal(day)
            dao.insertDrink(
                DrinkEntry(
                    dayEpoch = day,
                    amountMl = amountMl,
                    timestamp = instant.toEpochMilli()
                )
            )
        }
    }

    /**
     * Cập nhật mục tiêu lượng nước cho ngày hôm nay.
     *
     * @param targetMl Mục tiêu mới (phải thỏa mãn hàm kiểm tra [HydrationMath.validGoal]).
     */
    suspend fun setTodayGoal(targetMl: Int) {
        require(HydrationMath.validGoal(targetMl))
        db.withTransaction { dao.saveGoal(DailyGoal(today(), targetMl)) }
    }

    /**
     * Xóa một bản ghi uống nước theo ID.
     */
    suspend fun deleteDrink(id: Long) = dao.deleteDrink(id)
}