package com.example.aquadaily.domain

import kotlin.math.roundToInt

object HydrationMath {
    /** Lượng nước mục tiêu mặc định hàng ngày (2000 ml). */
    const val DEFAULT_GOAL = 2000

    /**
     * Tính phần trăm hoàn thành mục tiêu nước uống (từ 0 đến 100%).
     *
     * @param totalMl Tổng lượng nước đã uống (ml).
     * @param goalMl Mục tiêu nước cần uống trong ngày (ml).
     */
    fun percentage(totalMl: Long, goalMl: Int): Int =
        if (goalMl <= 0) 0 else (totalMl.toDouble() / goalMl * 100).roundToInt().coerceIn(0, 100)

    /**
     * Tính lượng nước còn thiếu để đạt mục tiêu (tối thiểu là 0 ml).
     */
    fun remaining(totalMl: Long, goalMl: Int): Long = (goalMl.toLong() - totalMl).coerceAtLeast(0)

    /**
     * Chuyển đổi chuỗi văn bản người dùng nhập thành số nguyên dung tích ml.
     * Chỉ chấp nhận giá trị hợp lệ trong khoảng 1 đến 5000 ml.
     */
    fun parseAmount(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..5000 }

    /**
     * Kiểm tra mục tiêu hàng ngày có nằm trong giới hạn hợp lý hay không (250ml đến 10000ml).
     */
    fun validGoal(goalMl: Int): Boolean = goalMl in 250..10000

    /**
     * Tính số ngày liên tiếp (streak) người dùng đã hoàn thành chỉ tiêu uống nước.
     *
     * @param completedDays Tập hợp các ngày (epoch day) đã hoàn thành chỉ tiêu.
     * @param today Ngày hiện tại (epoch day).
     * @return Số ngày liên tiếp đã hoàn thành chỉ tiêu tính ngược từ hôm nay hoặc hôm qua.
     */
    fun streak(completedDays: Set<Long>, today: Long): Int {
        var day = if (today in completedDays) today else today - 1
        var count = 0
        while (day in completedDays) {
            count++; day--
        }
        return count
    }
}
