package com.example.datastoreexercise.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.datastoreexercise.R
import com.example.datastoreexercise.data.model.User
import com.example.datastoreexercise.databinding.ActivityMainBinding
import com.example.datastoreexercise.factory.IntentActionType
import com.example.datastoreexercise.factory.IntentFactoryImpl
import com.example.datastoreexercise.ui.adapter.UserAdapter
import com.example.datastoreexercise.ui.dialog.DeleteDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Màn hình chính của ứng dụng:
 * - Kế thừa BaseActivity để sử dụng UserViewModel.
 * - Hiển thị danh sách User bằng RecyclerView.
 * - Hỗ trợ tìm kiếm theo tên thời gian thực bằng SearchView.
 * - Điều hướng sang các màn hình Thêm, Sửa, Xem chi tiết bằng IntentFactory.
 */
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    // ActivityResultLauncher thay thế phương thức startActivityForResult đã deprecated
    private lateinit var addUserLauncher: ActivityResultLauncher<Intent>
    private lateinit var updateUserLauncher: ActivityResultLauncher<Intent>
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Áp dụng ViewBinding để truy cập view trực tiếp không cần findViewById
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        setupRecyclerView()
        collectFlows()
    }

    /**
     * Khởi tạo menu trên ActionBar, cấu hình SearchView để tìm kiếm người dùng.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchView = menu?.findItem(R.id.menu_item_search_user)?.actionView as? SearchView
            ?: return true
        searchView.queryHint = getString(R.string.hint_query)
        searchView.setQuery(viewModel.currentQuery, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Khi nhấn nút tìm kiếm trên bàn phím
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchUserByName(query.orEmpty())
                closeSoftKeyboard()
                return true
            }

            // Khi nội dung ô tìm kiếm bị xóa hết thì tải lại toàn bộ danh sách
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.clearSearch()
                }
                return true
            }
        })
        return true
    }

    /**
     * Tiện ích ẩn bàn phím ảo sau khi submit tìm kiếm.
     */
    private fun closeSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    /**
     * Cấu hình các launcher nhận kết quả từ Activity khác và thiết lập sự kiện các nút.
     */
    private fun setupListeners() {
        // Đăng ký nhận kết quả trả về khi thêm người dùng mới từ NewUserActivity
        addUserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { handleResult(it, EXTRA_RESULT_ADD_KEY) }

        // Đăng ký nhận kết quả trả về khi cập nhật người dùng từ UserUpdateActivity
        updateUserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { handleResult(it, EXTRA_RESULT_UPDATE_KEY) }

        // Nút FAB mở màn hình thêm người dùng
        binding.btnAddNew.setOnClickListener {
            val intent = IntentFactoryImpl<User>(this)
                .createIntent(IntentActionType.CREATE, null)
            addUserLauncher.launch(intent)
        }

        // Lắng nghe kết quả xác nhận xóa người dùng trả về từ DeleteDialogFragment
        supportFragmentManager.setFragmentResultListener(
            DeleteDialogFragment.REQUEST_KEY, this
        ) { _, result ->
            val user = User(
                result.getInt(DeleteDialogFragment.USER_ID),
                result.getString(DeleteDialogFragment.FULL_NAME).orEmpty(),
                result.getString(DeleteDialogFragment.EMAIL).orEmpty()
            )
            viewModel.deleteUser(user)
        }
    }

    /**
     * Thu thập (collect) các StateFlow và Flow từ ViewModel theo vòng đời an toàn (Lifecycle-aware).
     * repeatOnLifecycle(Lifecycle.State.STARTED): Coroutine chỉ chạy khi Activity ở trạng thái STARTED hoặc RESUMED,
     * tự động tạm dừng khi vào background (STOPPED) giúp tiết kiệm tài nguyên CPU và bộ nhớ.
     */
    private fun collectFlows() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Mỗi Flow vô hạn (infinite Flow) cần một coroutine con độc lập (child coroutine)
                launch {
                    viewModel.uiState.collect { state ->
                        adapter.updateData(state.users)
                        // Hiển thị số lượng tìm kiếm trên subtitle của ActionBar
                        supportActionBar?.subtitle =
                            if (state.query.isNotEmpty() && !state.isLoading) {
                                getString(R.string.search_result, state.users.size)
                            } else null
                    }
                }
                launch {
                    viewModel.deleteEvents.collect { success ->
                        showMessage(
                            if (success) R.string.message_delete_success
                            else R.string.message_delete_failed
                        )
                    }
                }
            }
        }
    }

    /**
     * Xử lý hiển thị thông báo kết quả từ Activity con (Add hoặc Update) gửi về.
     */
    private fun handleResult(result: ActivityResult, extraKey: String) {
        val messageId = result.data?.getIntExtra(extraKey, 0) ?: return
        if (messageId != 0) showMessage(messageId)
    }

    /**
     * Hiển thị thông báo Snackbar dưới chân màn hình.
     */
    private fun showMessage(messageId: Int) {
        Snackbar.make(binding.root, getString(messageId), Snackbar.LENGTH_LONG).show()
    }

    /**
     * Thiết lập RecyclerView với LinearLayoutManager, Divider trang trí và UserAdapter.
     */
    private fun setupRecyclerView() {
        binding.recyclerUser.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(listener = object : OptionMenuClickListener {
            override fun update(user: User) {
                val intent = IntentFactoryImpl<User>(this@MainActivity)
                    .createIntent(IntentActionType.EDIT, user)
                updateUserLauncher.launch(intent)
            }

            override fun delete(user: User) {
                DeleteDialogFragment.newInstance(user)
                    .show(supportFragmentManager, "delete_user")
            }

            override fun viewDetail(user: User) {
                val intent = IntentFactoryImpl<User>(this@MainActivity)
                    .createIntent(IntentActionType.VIEW_DETAIL, user)
                startActivity(intent)
            }
        })
        binding.recyclerUser.adapter = adapter
        // Thêm đường kẻ phân cách giữa các item
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        getDrawable(R.drawable.divider)?.let { divider.setDrawable(it) }
        binding.recyclerUser.addItemDecoration(divider)
    }

    /**
     * Interface lắng nghe các sự kiện bấm vào từng item trong danh sách người dùng.
     */
    interface OptionMenuClickListener {
        fun update(user: User)
        fun delete(user: User)
        fun viewDetail(user: User)
    }

    companion object {
        const val EXTRA_RESULT_ADD_KEY = "EXTRA_RESULT_ADD_KEY"
        const val EXTRA_RESULT_UPDATE_KEY = "EXTRA_RESULT_UPDATE_KEY"
    }
}