package com.example.aquadaily.ui.report

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.aquadaily.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * DialogFragment xác nhận việc xóa một lần uống nước khỏi lịch sử.
 *
 * Hiển thị thông tin dung tích (ml) và thời gian uống để người dùng xác nhận trước khi xóa,
 * trả ID bản ghi cần xóa qua [parentFragmentManager.setFragmentResult].
 */
class DeleteDrinkDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_title)
            .setMessage(
                getString(
                    R.string.delete_message,
                    args.getInt("amount"),
                    args.getString("time")
                )
            )
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    RESULT,
                    bundleOf("id" to args.getLong("id"))
                )
            }
            .create()
    }

    companion object {
        const val RESULT = "delete_drink_result"
        const val TAG = "delete_drink"

        /** Khởi tạo dialog xác nhận xóa với ID, lượng nước và thời gian uống. */
        fun newInstance(id: Long, amount: Int, time: String) = DeleteDrinkDialogFragment().apply {
            arguments = bundleOf("id" to id, "amount" to amount, "time" to time)
        }
    }
}

