package com.example.datastoreexercise.ui.dialog


import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.datastoreexercise.R
import com.example.datastoreexercise.data.model.User

/**
 * Hộp thoại (DialogFragment) hiển thị xác nhận trước khi xóa người dùng.
 * Sử dụng FragmentResultListener API của AndroidX để trả kết quả an toàn qua vòng đời (Lifecycle-safe).
 */
class DeleteDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(R.string.text_confirm_delete)
            .setPositiveButton(R.string.text_confirm) { _, _ ->
                // Khi người dùng bấm Confirm, gửi kết quả kèm thông tin User về cho MainActivity qua parentFragmentManager
                parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle(requireArguments()))
            }
            .setNegativeButton(R.string.text_cancel, null) // Nút Cancel không cần làm gì
            .create()
    }

    companion object {
        // Khóa định danh cho kết quả xác nhận xóa
        const val REQUEST_KEY = "confirm_delete"
        const val USER_ID = "user_id"
        const val FULL_NAME = "full_name"
        const val EMAIL = "email"

        /**
         * Factory method tạo mới instance của DeleteDialogFragment và truyền User qua arguments Bundle.
         */
        fun newInstance(user: User) = DeleteDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(USER_ID, user.id)
                putString(FULL_NAME, user.fullName)
                putString(EMAIL, user.email)
            }
        }
    }
}