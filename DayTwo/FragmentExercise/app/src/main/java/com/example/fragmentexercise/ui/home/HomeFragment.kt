package com.example.fragmentexercise.ui.home

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.fragmentexercise.R
import com.example.fragmentexercise.app.StudentApp
import com.example.fragmentexercise.common.DateUtils
import com.example.fragmentexercise.common.DateUtils.EXTRA_STUDENT_ID
import com.example.fragmentexercise.data.model.Student
import com.example.fragmentexercise.databinding.FragmentHomeBinding
import com.example.fragmentexercise.ui.detail.StudentDetailFragment
import com.example.fragmentexercise.ui.dialog.ConfirmDialogFragment
import com.example.fragmentexercise.ui.dialog.ConfirmDialogFragment.Companion.REQUEST_KEY
import com.example.fragmentexercise.ui.studentform.FormMode
import com.example.fragmentexercise.ui.studentform.StudentFormFragment
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {
    // todo: bổ sung các biến binding, adapter, viewModel cho fragment
    private var _binding: FragmentHomeBinding? = null

    // todo: khử null cho binding
    private val binding get() = _binding!!
    private lateinit var studentAdapter: StudentAdapter
    private val viewModel: StudentViewModel by viewModels {
        val repository = (requireActivity().application as StudentApp).studentRepository
        val factory = StudentViewModelFactory(repository)
        factory
    }

    // cho phép bật tắt đường phân tách giữa các phần tử
    private lateinit var toggleDeco: ToggleableDecoration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // todo: thiết lập view binding cho fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // todo: thực hiện lời gọi tới các hàm khởi tạo các thành phần cần thiết
        initSearchView()
        setupMenuActions()
        initRecyclerView()
        setupSwipeToDelete()
        setupSwipeRefresh()
        initListeners()
        observeStudentData()
        setupFragmentResultListeners()
    }

    private fun initSearchView() {
        val menu = binding.toolbarHome.menu
        val searchView = menu?.findItem(R.id.menu_search_student)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchStudents(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchStudents(newText.orEmpty())
                return true
            }
        })
    }

    // todo: thiết lập các hành động cho menu tùy chọn trên màn hình chính
    private fun setupMenuActions() {
        binding.toolbarHome.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_sort_by_name -> {
                    // ẩn đường phân tách giữa các phần tử
                    toggleDeco.enabled = false
                    binding.rvStudent.invalidateItemDecorations()
                    viewModel.sortBy(SortField.NAME)
                    true
                }

                R.id.menu_sort_by_id -> {
                    // ẩn đường phân tách giữa các phần tử
                    toggleDeco.enabled = false
                    binding.rvStudent.invalidateItemDecorations()
                    viewModel.sortBy(SortField.ID)
                    true
                }

                R.id.menu_sort_by_gpa -> {
                    // ẩn đường phân tách giữa các phần tử
                    toggleDeco.enabled = false
                    binding.rvStudent.invalidateItemDecorations()
                    viewModel.sortBy(SortField.GPA)
                    true
                }

                else -> false
            }
        }
    }

    private fun initRecyclerView() {
        studentAdapter = StudentAdapter(
            listener = object : StudentAdapter.OnItemClickListener {
                override fun onItemClick(student: Student) {
                    navigateToDetailsScreen(student)
                }
            },
            // todo: bổ sung callback xử lý sự kiện nhấn menu tùy chọn trên từng phần tử student
            onStudentMenuClickListener = object : OnStudentMenuClickListener {
                override fun onEdit(student: Student) {
                    navigateToStudentFormScreen(student, FormMode.UPDATE.name)
                }

                override fun onDelete(student: Student) {
                    showConfirmDialog(student)
                }

                override fun onViewDetail(student: Student) {
                    navigateToDetailsScreen(student)
                }
            }
        )
        // todo: thêm divider có thể bật, tắt cho RecyclerView
        val baseDivider = DividerItemDecoration(
            requireContext(),
            RecyclerView.VERTICAL
        ).apply {
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.divider
            )?.let { setDrawable(it) }
        }
        toggleDeco = ToggleableDecoration(baseDivider)
        binding.rvStudent.adapter = studentAdapter
        binding.rvStudent.addItemDecoration(toggleDeco)
    }

    // todo: hiển thị hộp thoại xác nhận xóa
    private fun showConfirmDialog(student: Student) {
        ConfirmDialogFragment.newInstance(
            R.string.confirm_delete_title,
            R.string.confirm_delete_message,
            student.id
        ).show(parentFragmentManager, "ConfirmDialogFragment")
    }

    // todo: triển khai logic chức năng vuốt trái, phải để xóa
    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // cho phép vuốt phần tử sang trái, phải
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // không xử lý kéo thả trong trường hợp này
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val student = studentAdapter.getStudentAt(position)
                student?.let {
                    showConfirmDialog(it)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvStudent)
    }

    // todo: triển khai chức năng vuốt xuống để làm mới
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // ẩn đường phân tách giữa các phần tử
            toggleDeco.enabled = false
            binding.rvStudent.invalidateItemDecorations()
            viewModel.loadStudents()
        }
    }

    // todo: tạo hàm điều hướng đến màn hình StudentDetailFragment
    private fun navigateToDetailsScreen(student: Student) {
        val bundle = Bundle().apply {
            putString(EXTRA_STUDENT_ID, student.id)
        }
        parentFragmentManager
            .beginTransaction()
            .setCustomAnimations( // thiết lập animation khi chuyển đổi fragment
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(
                R.id.fcv_student,
                StudentDetailFragment::class.java,
                bundle
            )
            .addToBackStack(null)
            .commit()
    }

    // todo: tạo hàm điều hướng đến màn hình StudentFormFragment
    private fun navigateToStudentFormScreen(student: Student?, formMode: String?) {
        val bundle = Bundle().apply {
            putString(EXTRA_STUDENT_ID, student?.id)
            putString(DateUtils.EXTRA_FORM_MODE, formMode)
        }
        requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fcv_student, StudentFormFragment::class.java, bundle)
            .addToBackStack(null)
            .commit()
    }

    private fun initListeners() {
        binding.btnAdd.setOnClickListener {
            navigateToStudentFormScreen(formMode = FormMode.ADD.name, student = null)
        }
    }

    // todo: giám sát dữ liệu trả về từ viewModel
    private fun observeStudentData() {
        viewModel.students.observe(viewLifecycleOwner) {
            studentAdapter.updateStudents(it)
            if (it.isEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
            } else {
                binding.tvNoData.visibility = View.GONE
                binding.rvStudent.scrollToPosition(0)
                // khi có dữ liệu hoặc lỗi, tắt hiệu ứng swipe refresh
                if (binding.swipeRefresh.isRefreshing) {
                    binding.swipeRefresh.isRefreshing = false
                }
                // hiển thị đường phân tách giữa các phần tử
                toggleDeco.enabled = true
                binding.rvStudent.invalidateItemDecorations()
            }
        }

        viewModel.isStudentDeleted.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                val deletedMessage = getString(
                    R.string.delete_student_success,
                    viewModel.pendingDelete.value?.student?.id
                )
                Snackbar.make(
                    binding.root,
                    deletedMessage,
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.action_undo) {
                    viewModel.undoDelete()
                }.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        // nếu hết hạn chờ khôi phục thì reset trạng thái xóa thành công của phiên trước
                        viewModel.resetStatus()
                    }
                }).show()
            }
        }

        viewModel.isStudentRestored.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Snackbar.make(
                    binding.root,
                    R.string.restore_student_success,
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.resetStatus()
            }
        }
    }

    // todo: thiết lập fragment result listener để lắng nghe kết quả trả về từ các fragment con
    private fun setupFragmentResultListeners() {
        // chú ý sử dụng cùng fragment manager thì mới bắt được sự kiện
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val addStudentStatus = bundle.getString(StudentFormFragment.RESULT_KEY_STATUS)
            if (addStudentStatus == StudentFormFragment.STATUS_SUCCESS) {
                Snackbar.make(
                    binding.root,
                    R.string.add_student_success,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // todo: lắng nghe sự kiện xác nhận xóa sinh viên
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val studentId = bundle.getString(ConfirmDialogFragment.BUNDLE_STUDENT_ID)
            studentId?.let {
                viewModel.deleteStudentById(it)
            }
        }
    }

    // todo: hủy liên kết view binding trong onDestroyView
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}