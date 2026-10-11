package com.example.aquadaily.ui.infor

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel


/**
 * ViewModel phục vụ màn hình thông tin kiến thức ([InforFragment]).
 *
 * Chức năng:
 * - Cung cấp danh sách các chuyên mục kiến thức ([ArticleCatalog.sections]).
 * - Lưu và khôi phục trạng thái cuộn của danh sách chính bên ngoài (`outer_scroll`)
 *   và các danh sách con cuộn ngang (`section_$sectionId`) thông qua [SavedStateHandle]
 *   giúp giữ nguyên vị trí xem khi xoay màn hình hoặc khi bị hệ thống thu hồi bộ nhớ.
 */
class InforViewModel(private val saved: SavedStateHandle) : ViewModel() {
    /** Danh sách tất cả các chuyên mục thông tin. */
    val sections = ArticleCatalog.sections

    /** Lưu trạng thái cuộn của một chuyên mục cụ thể. */
    fun saveScroll(sectionId: Long, state: Parcelable) {
        saved["section_$sectionId"] = state
    }

    /** Đọc lại trạng thái cuộn của một chuyên mục cụ thể. */
    fun readScroll(sectionId: Long): Parcelable? = saved["section_$sectionId"]

    /** Lưu trạng thái cuộn của danh sách chính (dọc). */
    fun saveOuterScroll(state: Parcelable) {
        saved["outer_scroll"] = state
    }

    /** Đọc lại trạng thái cuộn của danh sách chính (dọc). */
    fun readOuterScroll(): Parcelable? = saved["outer_scroll"]
}