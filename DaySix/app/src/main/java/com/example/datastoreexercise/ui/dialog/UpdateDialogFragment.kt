package com.example.datastoreexercise.ui.dialog
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.datastoreexercise.R

/**
 * Hộp thoại (DialogFragment) hiển thị xác nhận trước khi cập nhật thông tin người dùng.
 * Trả về kết quả qua FragmentResultListener API.
 */
class UpdateDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(R.string.text_confirm_update)
            .setPositiveButton(R.string.text_confirm) { _, _ -> sendResult(true) }
            .setNegativeButton(R.string.text_cancel) { _, _ -> sendResult(false) }
            .create()
    }

    /**
     * Gửi kết quả xác nhận (confirmed: true/false) cùng thông tin form về cho UserUpdateActivity.
     */
    private fun sendResult(confirmed: Boolean) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle(requireArguments()).apply { putBoolean(CONFIRMED, confirmed) }
        )
    }

    companion object {
        const val REQUEST_KEY = "confirm_update"
        const val CONFIRMED = "confirmed"
        const val FULL_NAME = "full_name"
        const val EMAIL = "email"

        /**
         * Factory method tạo mới dialog và gắn thông tin họ tên, email vào arguments Bundle.
         */
        fun newInstance(fullName: String, email: String) = UpdateDialogFragment().apply {
            arguments = Bundle().apply {
                putString(FULL_NAME, fullName)
                putString(EMAIL, email)
            }
        }
    }
}