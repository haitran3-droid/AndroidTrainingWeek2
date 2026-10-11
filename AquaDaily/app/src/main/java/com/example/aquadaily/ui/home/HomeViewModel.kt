package com.example.aquadaily.ui.home

import androidx.lifecycle.viewModelScope
import com.example.aquadaily.data.HydrationRepository
import com.example.aquadaily.domain.HydrationMath
import com.example.aquadaily.ui.ActionViewModel
import com.example.aquadaily.ui.DayUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý màn hình chính (Home Screen).
 *
 * Nhiệm vụ chính:
 * - Quan sát lượng nước và mục tiêu của ngày hiện tại dưới dạng [StateFlow].
 * - Tự động phát hiện khi bước sang ngày mới (kiểm tra mỗi 60 giây).
 * - Cung cấp các hành động: thêm lần uống nước ([addDrink]), sửa mục tiêu ngày ([setGoal]), thử lại khi gặp lỗi ([retry]).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: HydrationRepository) : ActionViewModel() {
    /** Ngày hiện tại đang được theo dõi (epoch day). */
    private val day = MutableStateFlow(repository.today())

    /** Bộ đếm kích hoạt tải lại dữ liệu khi người dùng ấn nút Retry. */
    private val reload = MutableStateFlow(0)

    /**
     * Luồng kết hợp thông tin chi tiết ngày và chuỗi ngày hoàn thành (streak) từ Repository.
     * Sử dụng [flatMapLatest] để tự động hủy luồng cũ và chuyển sang lắng nghe ngày mới khi `day` hoặc `reload` thay đổi.
     */
    private val data = combine(day, reload) { date, _ -> date }.flatMapLatest { date ->
        combine(
            repository.observeDay(date),
            repository.observeCompletedDays()
        ) { snapshot, completed ->
            DayUiState(
                date,
                snapshot.goalMl,
                snapshot.entries,
                HydrationMath.streak(completed.toSet(), date),
                loading = false
            )
        }.catch { emit(DayUiState(date, loading = false, error = true)) }
    }

    /**
     * Trạng thái UI hoàn chỉnh của màn hình chính được kết hợp từ dữ liệu nghiệp vụ (`data`)
     * và trạng thái ghi tác vụ (`operation`), chia sẻ an toàn qua [stateIn].
     */
    val state = combine(data, operation) { state, op ->
        state.copy(
            busy = op.busy,
            error = state.error || op.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DayUiState(repository.today())
    )

    init {
        refreshDay()
        // Kiểm tra mỗi 60 giây để tự động chuyển giao diện sang ngày mới khi qua nửa đêm
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (day.value != repository.today()) refreshDay()
            }
        }
    }

    /** Làm mới mốc ngày hiện tại và đảm bảo CSDL đã có mục tiêu cho ngày hôm nay. */
    fun refreshDay() {
        day.value = repository.today()
        perform { repository.ensureToday() }
    }

    /** Thử lại tải dữ liệu khi có lỗi phát sinh. */
    fun retry() {
        operation.update { it.copy(error = false) }
        reload.update { it + 1 }
        refreshDay()
    }

    /** Thêm lượng nước vừa uống vào ngày hôm nay. */
    fun addDrink(amountMl: Int) =
        perform {
            repository.addDrink(amountMl)
            day.value = repository.today()
        }

    /** Cập nhật mục tiêu lượng nước cho ngày hôm nay. */
    fun setGoal(targetMl: Int) =
        perform {
            repository.setTodayGoal(targetMl)
            day.value = repository.today()
        }
}