package com.example.aquadaily.ui.report

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aquadaily.AquaDailyApp
import com.example.aquadaily.MainActivity
import com.example.aquadaily.R
import com.example.aquadaily.databinding.FragmentReportBinding
import com.example.aquadaily.ui.DayUiState
import com.example.aquadaily.ui.reportFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị màn hình báo cáo lịch sử uống nước theo từng ngày.
 *
 * Chức năng:
 * - Hiển thị ngày được chọn, thanh tiến độ tổng thể, tổng lượng nước và số lần uống.
 * - Danh sách chi tiết các lần uống nước qua [DrinkHistoryAdapter].
 * - Mở [CalendarDialogFragment] để chọn ngày khác trên lịch.
 * - Mở [DeleteDrinkDialogFragment] khi người dùng muốn xóa một lần uống.
 * - Nút chuyển nhanh về trang chủ ([MainActivity.goHome]).
 */
class ReportFragment : Fragment(R.layout.fragment_report) {
    private var binding: FragmentReportBinding? = null
    private var adapter: DrinkHistoryAdapter? = null
    private val model: ReportViewModel by viewModels {
        reportFactory((requireActivity().application as AquaDailyApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentReportBinding.bind(view)
        binding = ui

        adapter = DrinkHistoryAdapter { row, time ->
            val manager = childFragmentManager
            if (!manager.isStateSaved && manager.findFragmentByTag(DeleteDrinkDialogFragment.TAG) == null) {
                DeleteDrinkDialogFragment.newInstance(row.id, row.amountMl, time)
                    .showNow(manager, DeleteDrinkDialogFragment.TAG)
            }
        }
        ui.history.layoutManager = LinearLayoutManager(requireContext())
        ui.history.adapter = adapter

        // Lắng nghe chọn ngày từ CalendarDialogFragment
        childFragmentManager.setFragmentResultListener(
            CalendarDialogFragment.RESULT,
            viewLifecycleOwner
        ) { _, result -> model.selectDay(result.getLong("day")) }

        // Lắng nghe xác nhận xóa từ DeleteDrinkDialogFragment
        childFragmentManager.setFragmentResultListener(
            DeleteDrinkDialogFragment.RESULT,
            viewLifecycleOwner
        ) { _, result -> model.deleteDrink(result.getLong("id")) }

        ui.selectDate.setOnClickListener {
            val manager = childFragmentManager
            if (!manager.isStateSaved && manager.findFragmentByTag(CalendarDialogFragment.TAG) == null) {
                CalendarDialogFragment.newInstance(model.selectedDay.value)
                    .showNow(manager, CalendarDialogFragment.TAG)
            }
        }

        ui.goHome.setOnClickListener { (requireActivity() as MainActivity).goHome() }
        ui.error.setOnClickListener { model.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect(::render)
            }
        }
    }

    /** Cập nhật giao diện theo trạng thái ngày [DayUiState]. */
    private fun render(state: DayUiState) {
        val ui = binding ?: return
        ui.selectDate.text = LocalDate.ofEpochDay(state.dayEpoch)
            .format(DateTimeFormatter.ofPattern("d 'tháng' M, yyyy", Locale.forLanguageTag("vi")))
        ui.selectDate.contentDescription =
            getString(R.string.choose_date) + ", " + ui.selectDate.text
        ui.dayTotal.text = getString(R.string.day_total, state.totalMl)
        ui.dayCount.text = getString(R.string.day_count, state.entries.size, state.goalMl)
        ui.dayProgress.setProgressCompat(state.percent, false)
        ui.loading.isVisible = state.loading || state.busy
        ui.empty.isVisible = !state.loading && !state.error && state.entries.isEmpty()
        ui.error.isVisible = state.error
        adapter?.submitList(state.entries)
    }

    override fun onDestroyView() {
        binding?.history?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }
}

