package com.example.datastoreexercise.ui.updateuser

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datastoreexercise.ui.BaseActivity
import com.example.datastoreexercise.ui.MainActivity
import com.example.datastoreexercise.R
import com.example.datastoreexercise.databinding.ActivityUpdateUserBinding
import com.example.datastoreexercise.ui.dialog.UpdateDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
/**
 * Màn hình cập nhật thông tin người dùng:
 * - Kế thừa BaseActivity để tái sử dụng UserViewModel.
 * - Tái sử dụng layout activity_new_user.xml thông qua thẻ <include>.
 * - Xử lý khéo léo biến formFilled và onSaveInstanceState để khi xoay màn hình (orientation change),
 *   dữ liệu người dùng đang nhập dở không bị ghi đè bởi dữ liệu cũ từ database.
 */
class UserUpdateActivity : BaseActivity() {
    private lateinit var binding: ActivityUpdateUserBinding
    private var userId: Int = 0
    private var formFilled = false // Cờ đánh dấu form đã được nạp dữ liệu ban đầu từ DB hay chưa

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Đổi nhãn nút từ "Add" sang "Update"
        binding.containerLayout.btnAdd.text = getString(R.string.text_btn_update)
        formFilled = savedInstanceState?.getBoolean(STATE_FORM_FILLED) ?: false

        // Lấy ID người dùng được truyền qua Intent
        userId = intent.getIntExtra(EXTRA_ID, 0)
        if (userId <= 0) {
            finish()
            return
        }

        binding.containerLayout.btnAdd.isEnabled = formFilled
        // Yêu cầu ViewModel nạp thông tin người dùng theo userId
        viewModel.loadUser(userId)
        setupListeners()
        collectFlows()
    }

    /**
     * Thiết lập các sự kiện lắng nghe:
     * - Kết quả từ UpdateDialogFragment.
     * - Click nút Update mở hộp thoại xác nhận.
     * - Click nút Cancel để đóng Activity.
     */
    private fun setupListeners() {
        // Lắng nghe kết quả từ UpdateDialogFragment qua FragmentResultListener
        supportFragmentManager.setFragmentResultListener(
            UpdateDialogFragment.REQUEST_KEY, this
        ) { _, result ->
            if (result.getBoolean(UpdateDialogFragment.CONFIRMED)) {
                // Người dùng xác nhận -> Thực hiện cập nhật vào database
                viewModel.updateUser(
                    userId,
                    result.getString(UpdateDialogFragment.FULL_NAME).orEmpty(),
                    result.getString(UpdateDialogFragment.EMAIL).orEmpty()
                )
            } else {
                finish()
            }
        }

        // Bấm Update -> Mở Dialog xác nhận
        binding.containerLayout.btnAdd.setOnClickListener {
            UpdateDialogFragment.newInstance(
                binding.containerLayout.editFullname.text.toString(),
                binding.containerLayout.editEmail.text.toString()
            ).show(supportFragmentManager, "update_user")
        }

        binding.containerLayout.btnCancel.setOnClickListener { finish() }
    }

    /**
     * Thu thập dữ liệu từ ViewModel:
     * - selectedUser: Điền dữ liệu vào form (chỉ điền lần đầu khi !formFilled để không đè dữ liệu đang gõ).
     * - isUpdating: Khóa nút Update khi đang trong tiến trình lưu.
     * - updateEvents: Nhận kết quả thành công/thất bại, trả kết quả về MainActivity và đóng màn hình.
     */
    private fun collectFlows() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedUser.collect { user ->
                        // Chỉ điền dữ liệu khi form chưa được điền lần nào
                        if (user != null && !formFilled) {
                            binding.containerLayout.editFullname.setText(user.fullName)
                            binding.containerLayout.editEmail.setText(user.email)
                            formFilled = true
                            binding.containerLayout.btnAdd.isEnabled = !viewModel.isUpdating.value
                        }
                    }
                }
                launch {
                    viewModel.isUpdating.collect { updating ->
                        binding.containerLayout.btnAdd.isEnabled = formFilled && !updating
                    }
                }
                launch {
                    viewModel.updateEvents.collect { success ->
                        val messageId = if (success) R.string.message_update_success
                        else R.string.message_update_failed
                        val result = Intent().apply {
                            putExtra(MainActivity.EXTRA_RESULT_UPDATE_KEY, messageId)
                        }
                        setResult(if (success) RESULT_OK else RESULT_CANCELED, result)
                        finish()
                    }
                }
            }
        }
    }

    /**
     * Lưu trạng thái cờ formFilled khi cấu hình thay đổi (ví dụ: xoay màn hình).
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_FORM_FILLED, formFilled)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_ID = "net.braniumacademy.kotlin.ui.updateuser.EXTRA_ID"
        private const val STATE_FORM_FILLED = "form_filled"
    }
}
