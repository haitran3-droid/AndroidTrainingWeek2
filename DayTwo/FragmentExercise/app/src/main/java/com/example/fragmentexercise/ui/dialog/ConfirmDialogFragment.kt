package com.example.fragmentexercise.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ConfirmDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleId = arguments?.getInt(ARG_TITLE) ?: 0
        val messageId = arguments?.getInt(ARG_MESSAGE) ?: 0
        val studentId = arguments?.getString(ARG_STUDENT_ID) ?: ""
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleId)
            .setMessage(getString(messageId, studentId))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putString(BUNDLE_STUDENT_ID, studentId)
                    }
                )
            }
            .create()
    }

    companion object{
        const val REQUEST_KEY = "confirm_dialog"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_STUDENT_ID = "arg_student_id"
        const val BUNDLE_STUDENT_ID = "bundle_student_id"

        fun newInstance(titleId: Int, messageId: Int, studentId: String): ConfirmDialogFragment {
            return ConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TITLE, titleId)
                    putInt(ARG_MESSAGE, messageId)
                    putString(ARG_STUDENT_ID, studentId)
                }
            }
        }
    }
}