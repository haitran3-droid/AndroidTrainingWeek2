package com.example.datastoreexercise.ui.detail
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datastoreexercise.ui.BaseActivity
import com.example.datastoreexercise.databinding.ActivityUserDetailBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
/**
 * Màn hình xem chi tiết thông tin người dùng (chỉ đọc):
 * - Kế thừa BaseActivity để sử dụng UserViewModel.
 * - Hiển thị tên và email của người dùng tương ứng với EXTRA_ID.
 */
class UserDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityUserDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy ID người dùng từ Intent
        val userId = intent.getIntExtra(EXTRA_ID, 0)
        if (userId <= 0) {
            finish()
            return
        }

        // Tải thông tin người dùng từ ViewModel
        viewModel.loadUser(userId)

        // Lắng nghe dữ liệu người dùng được phát ra từ StateFlow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedUser.collect { user ->
                    binding.editNameDetail.setText(user?.fullName)
                    binding.editEmailDetail.setText(user?.email)
                }
            }
        }

        // Nút OK đóng màn hình chi tiết
        binding.btnOk.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_ID = "net.braniumacademy.kotlin.ui.detail.EXTRA_ID"
    }
}