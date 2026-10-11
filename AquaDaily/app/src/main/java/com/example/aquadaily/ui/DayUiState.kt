package com.example.aquadaily.ui

import com.example.aquadaily.data.DrinkEntry
import com.example.aquadaily.domain.HydrationMath

/**
 * Trạng thái giao diện (UI State) đại diện cho toàn bộ thông tin hiển thị của một ngày.
 *
 * Tuân theo kiến trúc MVI / UDF (Unidirectional Data Flow), gom mọi thuộc tính UI vào một state bất biến (immutable).
 *
 * @property dayEpoch Ngày đang xét (dạng epoch day).
 * @property goalMl Mục tiêu lượng nước trong ngày (ml).
 * @property entries Danh sách các bản ghi uống nước trong ngày.
 * @property streak Số ngày hoàn thành mục tiêu liên tiếp tính đến ngày này.
 * @property loading Cờ thể hiện trạng thái đang tải dữ liệu ban đầu.
 * @property busy Cờ thể hiện trạng thái đang bận thực hiện tác vụ ghi/xóa vào CSDL.
 * @property error Cờ báo lỗi khi truy vấn hoặc thực hiện tác vụ thất bại.
 */
data class DayUiState(
    val dayEpoch: Long,
    val goalMl: Int = HydrationMath.DEFAULT_GOAL,
    val entries: List<DrinkEntry> = emptyList(),
    val streak: Int = 0,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: Boolean = false
) {
    /** Tổng lượng nước đã uống trong ngày (ml). */
    val totalMl get() = entries.sumOf { it.amountMl.toLong() }

    /** Phần trăm mục tiêu đã đạt được (0 - 100%). */
    val percent get() = HydrationMath.percentage(totalMl, goalMl)

    /** Lượng nước còn lại cần nạp để đạt mục tiêu (ml). */
    val remaining get() = HydrationMath.remaining(totalMl, goalMl)
}