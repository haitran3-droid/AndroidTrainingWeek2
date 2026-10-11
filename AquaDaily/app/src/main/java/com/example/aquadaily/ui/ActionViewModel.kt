package com.example.aquadaily.ui

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Trạng thái của một tác vụ ghi dữ liệu (Operation).
 *
 * @property busy Đang có ít nhất một tác vụ ngầm đang chạy.
 * @property error Có lỗi xảy ra trong quá trình ghi dữ liệu (ví dụ: lỗi SQLite hoặc I/O).
 */
data class Operation(val busy: Boolean = false, val error: Boolean = false)

/**
 * Lớp ViewModel cơ sở (Base ViewModel) quản lý việc thực thi các tác vụ ghi/sửa dữ liệu bất đồng bộ.
 *
 * Sử dụng [Mutex] để tuần tự hóa (serialize) các thao tác ghi vào CSDL, ngăn ngừa race condition,
 * đồng thời theo dõi số lượng tác vụ đang chờ thông qua biến đếm `pending`.
 */
abstract class ActionViewModel : ViewModel() {
    /** Luồng trạng thái theo dõi hoạt động ghi dữ liệu. */
    protected val operation = MutableStateFlow(Operation())

    /** Khóa Mutex đảm bảo chỉ một tác vụ ghi CSDL được chạy tại một thời điểm. */
    private val writeLock = Mutex()

    /** Số lượng tác vụ đang chờ xử lý hoặc đang chạy. */
    private var pending = 0

    /**
     * Thực thi một tác vụ suspend an toàn trên [viewModelScope].
     *
     * - Bật cờ `busy = true` khi bắt đầu.
     * - Sử dụng [writeLock] để tuần tự hóa việc ghi.
     * - Bắt các lỗi [SQLiteException] và [IOException] để cập nhật cờ `error = true`.
     * - Không nuốt [CancellationException] để tuân thủ cơ chế hủy coroutine chuẩn.
     * - Cập nhật cờ `busy = false` khi tất cả tác vụ kết thúc (`pending == 0`).
     */
    @MainThread
    protected fun perform(action: suspend () -> Unit) {
        pending++
        operation.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try {
                writeLock.withLock { action() }; operation.update { it.copy(error = false) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: SQLiteException) {
                operation.update { it.copy(error = true) }
            } catch (error: IOException) {
                operation.update { it.copy(error = true) }
            } finally {
                pending--; operation.update { it.copy(busy = pending > 0) }
            }
        }
    }
}