package com.example.aquadaily.ui.report

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.aquadaily.R
import java.time.LocalDate

/**
 * DialogFragment cho phép người dùng chọn một ngày cụ thể trên lịch để xem lịch sử uống nước.
 *
 * Chức năng:
 * - Hiển thị [DatePickerDialog] với ngày chọn tối đa là ngày hôm nay (`maxDate = System.currentTimeMillis()`).
 * - Trả kết quả ngày đã chọn (dưới dạng epoch day) qua [parentFragmentManager.setFragmentResult].
 * - Lưu và khôi phục ngày đang chọn khi xoay màn hình qua `onSaveInstanceState`.
 */
class CalendarDialogFragment : DialogFragment() {
    private var picker: DatePickerDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val initial = LocalDate.ofEpochDay(
            savedInstanceState?.getLong("picker_day") ?: requireArguments().getLong("day")
        )
        return DatePickerDialog(requireContext(), { _, year, month, day ->
            parentFragmentManager.setFragmentResult(
                RESULT,
                bundleOf("day" to LocalDate.of(year, month + 1, day).toEpochDay())
            )
        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setTitle(R.string.calendar_title)
            picker = this
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        picker?.datePicker?.let {
            outState.putLong(
                "picker_day",
                LocalDate.of(it.year, it.month + 1, it.dayOfMonth).toEpochDay()
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        picker = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT = "calendar_day_result"
        const val TAG = "history_calendar"

        /** Khởi tạo dialog với ngày ban đầu được chọn. */
        fun newInstance(day: Long) =
            CalendarDialogFragment().apply { arguments = bundleOf("day" to day) }
    }
}

