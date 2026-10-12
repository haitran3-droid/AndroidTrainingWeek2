package com.example.datastoreexercise.ui.newuser

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.datastoreexercise.ui.MainActivity
import com.example.datastoreexercise.R
import com.example.datastoreexercise.data.database.AppDatabase
import com.example.datastoreexercise.data.repository.UserRepository
import com.example.datastoreexercise.databinding.ActivityNewUserBinding
import com.example.datastoreexercise.ui.viewmodel.NewUserViewModel
import com.example.datastoreexercise.ui.viewmodel.UserViewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
/**
 * Màn hình nhập thông tin người dùng mới:
 * - Sử dụng NewUserViewModel để xử lý logic thêm mới.
 * - Khóa nút 'Add' khi đang trong tiến trình lưu để chống bấm liên tục.
 * - Trả kết quả (RESULT_OK / RESULT_CANCELED) về cho MainActivity qua setResult.
 */
class NewUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewUserBinding
    // Khởi tạo NewUserViewModel bằng Factory
    private val viewModel: NewUserViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        UserViewModelFactory(UserRepository(database.getUserDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sự kiện khi bấm nút Add
        binding.btnAdd.setOnClickListener {
            viewModel.saveUser(
                binding.editFullname.text.toString(),
                binding.editEmail.text.toString()
            )
        }

        // Sự kiện khi bấm nút Cancel
        binding.btnCancel.setOnClickListener { finish() }

        // Thu thập các Flow phát ra từ ViewModel an toàn theo vòng đời
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Vô hiệu hóa nút Add nếu đang thực hiện lưu
                launch {
                    viewModel.isSaving.collect { saving ->
                        binding.btnAdd.isEnabled = !saving
                    }
                }
                // Nhận sự kiện kết quả lưu từ Channel
                launch {
                    viewModel.saveEvents.collect { success ->
                        val messageId = if (success) R.string.message_success else R.string.message_failed
                        val result = Intent().apply {
                            putExtra(MainActivity.EXTRA_RESULT_ADD_KEY, messageId)
                        }
                        setResult(if (success) RESULT_OK else RESULT_CANCELED, result)
                        finish()
                    }
                }
            }
        }
    }
}