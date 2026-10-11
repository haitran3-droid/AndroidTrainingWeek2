package com.example.aquadaily.ui.home

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.aquadaily.R
import com.example.aquadaily.databinding.DialogAmountBinding
import com.example.aquadaily.domain.HydrationMath
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * DialogFragment cho phép người dùng nhập lượng nước đã uống hoặc cập nhật mục tiêu trong ngày.
 *
 * - Khi `isGoal == true`: Nhập mục tiêu uống nước mới (hỗ trợ kiểm tra hợp lệ từ 250ml đến 10000ml).
 * - Khi `isGoal == false`: Nhập lượng nước vừa uống (hỗ trợ nút chọn nhanh 150ml, 250ml, 500ml).
 * - Giao tiếp trả kết quả về Fragment cha thông qua [parentFragmentManager.setFragmentResult].
 */
class AmountDialogFragment : DialogFragment() {
    private var binding: DialogAmountBinding? = null
    private val isGoal get() = requireArguments().getBoolean("is_goal")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ui = DialogAmountBinding.inflate(layoutInflater)
        binding = ui
        // Ẩn các nút preset (150, 250, 500ml) nếu đang ở chế độ chỉnh sửa mục tiêu
        ui.presets.isVisible = !isGoal
        ui.hint.setText(if (isGoal) R.string.goal_hint else R.string.amount_prompt)
        ui.inputLayout.setHint(if (isGoal) R.string.goal_input_label else R.string.volume_label)
        ui.amountInput.setText(
            savedInstanceState?.getString("input") ?: requireArguments().getInt("initial")
                .toString()
        )

        fun preset(amount: Int) {
            ui.amountInput.setText(amount.toString())
        }
        ui.amount150.setOnClickListener { preset(150) }
        ui.amount250.setOnClickListener { preset(250) }
        ui.amount500.setOnClickListener { preset(500) }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isGoal) R.string.goal_title else R.string.add_title)
            .setView(ui.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        // Gắn listener thủ công cho nút Positive để ngăn Dialog tự động đóng khi validation thất bại
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                val ui = binding ?: return@setOnClickListener
                val text = ui.amountInput.text.toString()
                val value = if (isGoal) {
                    text.trim().toIntOrNull()?.takeIf(HydrationMath::validGoal)
                } else {
                    HydrationMath.parseAmount(text)
                }

                if (value == null) {
                    ui.inputLayout.error =
                        getString(if (isGoal) R.string.goal_error else R.string.amount_error)
                } else {
                    parentFragmentManager.setFragmentResult(
                        RESULT,
                        bundleOf("amount" to value, "is_goal" to isGoal)
                    )
                    dismiss()
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("input", binding?.amountInput?.text?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
    companion object {
        const val RESULT = "water_amount_result"
        const val TAG = "water_amount"

        /**
         * Tạo mới một instance của dialog.
         * @param goal `true` nếu dùng để sửa mục tiêu ngày, `false` nếu dùng để thêm lần uống nước.
         * @param initial Giá trị khởi tạo hiển thị trong ô nhập.
         */
        fun newInstance(goal: Boolean, initial: Int) = AmountDialogFragment().apply {
            arguments = bundleOf("is_goal" to goal, "initial" to initial)
        }
    }
}