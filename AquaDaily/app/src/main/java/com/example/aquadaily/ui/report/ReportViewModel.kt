package com.example.aquadaily.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.aquadaily.data.HydrationRepository
import com.example.aquadaily.ui.ActionViewModel
import com.example.aquadaily.ui.DayUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update


/**
 * ViewModel quản lý màn hình báo cáo & lịch sử uống nước ([ReportFragment]).
 *
 * Nhiệm vụ chính:
 * - Lưu trữ và phát ra mốc ngày được chọn ([selectedDay]) qua [SavedStateHandle].
 * - Không cho phép chọn ngày trong tương lai (`day <= repository.today()`).
 * - Quan sát danh sách uống nước và mục tiêu của ngày đó từ Repository.
 * - Cung cấp hành động xóa lần uống ([deleteDrink]) và thử lại ([retry]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val repository: HydrationRepository,
    private val saved: SavedStateHandle
) : ActionViewModel() {
    /** Ngày đang được chọn để xem báo cáo (mặc định là ngày hôm nay). */
    val selectedDay = saved.getStateFlow("selected_day", repository.today())

    /** Bộ đếm kích hoạt tải lại dữ liệu khi người dùng ấn nút Retry. */
    private val reload = MutableStateFlow(0)

    /** Luồng lấy dữ liệu ngày được chọn mỗi khi `selectedDay` hoặc `reload` thay đổi. */
    private val data = combine(selectedDay, reload) { day, _ -> day }.flatMapLatest { day ->
        repository.observeDay(day).map { DayUiState(day, it.goalMl, it.entries, loading = false) }
            .catch { emit(DayUiState(day, loading = false, error = true)) }
    }

    /** Trạng thái UI tổng hợp cho màn hình báo cáo. */
    val state = combine(data, operation) { state, op ->
        state.copy(
            busy = op.busy,
            error = state.error || op.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DayUiState(selectedDay.value)
    )

    /** Chọn ngày mới để xem báo cáo (chặn các ngày trong tương lai). */
    fun selectDay(day: Long) {
        if (day <= repository.today()) saved["selected_day"] = day
    }

    /** Xóa một lần uống nước dựa vào ID. */
    fun deleteDrink(id: Long) = perform { repository.deleteDrink(id) }

    /** Thử lại khi gặp lỗi tải dữ liệu. */
    fun retry() {
        operation.update { it.copy(error = false) }
        reload.update { it + 1 }
    }
}

