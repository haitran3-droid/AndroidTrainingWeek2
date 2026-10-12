package com.example.datastoreexercise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datastoreexercise.data.model.User
import com.example.datastoreexercise.data.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModel(private val repository: Repository) : ViewModel() {
    // StateFlow lưu trữ từ khóa tìm kiếm
    private val searchQuery = MutableStateFlow("")
    val currentQuery: String get() = searchQuery.value

    /**
     * uiState: StateFlow cung cấp trạng thái danh sách người dùng cho UI.
     * - flatMapLatest: Mỗi khi searchQuery thay đổi, Flow truy vấn trước đó sẽ bị hủy (cancel)
     *   và chuyển sang lắng nghe Flow truy vấn mới từ Room.
     * - onStart: Phát ra trạng thái loading khi bắt đầu thu thập Flow mới.
     * - stateIn: Biến cold Flow thành hot StateFlow gắn với viewModelScope.
     *   SharingStarted.WhileSubscribed(5_000) giúp giữ kết nối thêm 5 giây sau khi UI dừng (onStop),
     *   tránh việc huỷ và query lại không cần thiết khi xoay màn hình (orientation change).
     */
    val uiState: StateFlow<UserListUiState> = searchQuery.flatMapLatest { query ->
        val source = if (query.isEmpty()) repository.users else repository.findByName(query)
        source.map { users -> UserListUiState(users, query, isLoading = false) }
            .onStart { emit(UserListUiState(query = query)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserListUiState())

    // ID của người dùng đang được chọn để xem chi tiết hoặc cập nhật
    private val selectedUserId = MutableStateFlow<Int?>(null)

    /**
     * StateFlow phát ra đối tượng User tương ứng với selectedUserId.
     * Tự động cập nhật nếu thông tin người dùng đó trong DB thay đổi.
     */
    val selectedUser: StateFlow<User?> = selectedUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getUserById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Channel gửi sự kiện 1 lần (one-shot event) cho thao tác cập nhật (Update)
    private val updateResults = Channel<Boolean>(Channel.BUFFERED)
    val updateEvents: Flow<Boolean> = updateResults.receiveAsFlow()

    // Channel gửi sự kiện 1 lần cho thao tác xóa (Delete)
    private val deleteResults = Channel<Boolean>(Channel.BUFFERED)
    val deleteEvents: Flow<Boolean> = deleteResults.receiveAsFlow()

    // Trạng thái khóa nút Update khi đang tiến hành cập nhật
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    /**
     * Tải thông tin người dùng theo ID vào selectedUserId.
     */
    fun loadUser(id: Int) {
        selectedUserId.value = id
    }

    /**
     * Cập nhật từ khóa tìm kiếm theo tên.
     */
    fun searchUserByName(query: String) {
        searchQuery.value = query.trim()
    }

    /**
     * Xóa từ khóa tìm kiếm, hiển thị lại toàn bộ danh sách.
     */
    fun clearSearch() {
        searchQuery.value = ""
    }

    /**
     * Xóa người dùng khỏi database và gửi kết quả vào Channel deleteResults.
     */
    fun deleteUser(user: User) = viewModelScope.launch {
        deleteResults.send(operationSucceeded { repository.deleteUser(user) > 0 })
    }

    /**
     * Cập nhật thông tin người dùng trong database và gửi kết quả vào Channel updateResults.
     */
    fun updateUser(userId: Int, fullName: String, email: String) {
        if (_isUpdating.value) return
        _isUpdating.value = true
        viewModelScope.launch {
            try {
                updateResults.send(operationSucceeded {
                    repository.updateUser(User(userId, fullName, email)) > 0
                })
            } finally {
                _isUpdating.value = false
            }
        }
    }
}

/**
 * Hàm tiện ích thực thi khối lệnh database an toàn.
 * Lưu ý: Bắt buộc re-throw CancellationException để không làm hỏng cơ chế hủy Coroutine của Kotlin.
 */
internal suspend fun operationSucceeded(operation: suspend () -> Boolean): Boolean {
    return try {
        operation()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        false
    }
}
