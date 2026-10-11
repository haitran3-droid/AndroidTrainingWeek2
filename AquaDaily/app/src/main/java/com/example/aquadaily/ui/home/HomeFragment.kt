package com.example.aquadaily.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aquadaily.AquaDailyApp
import com.example.aquadaily.R
import com.example.aquadaily.databinding.FragmentHomeBinding
import com.example.aquadaily.ui.DayUiState
import com.example.aquadaily.ui.homeFactory
import kotlinx.coroutines.launch

/**
 * Fragment giao diện trang chủ của ứng dụng AquaDaily.
 *
 * Chức năng:
 * - Hiển thị vòng tròn tiến độ ([com.example.aquadaily.ui.widgets.GoalProgressView]) và hiệu ứng sóng nước ([com.example.aquadaily.ui.widgets.WaveView]).
 * - Thống kê lượng nước đã uống, mục tiêu, lượng còn thiếu và chuỗi ngày streak.
 * - Cho phép mở dialog thêm nước hoặc chỉnh sửa mục tiêu thông qua [AmountDialogFragment].
 * - Quản lý vòng đời animation sóng nước theo trạng thái hiển thị của Fragment (`onResume`/`onPause`).
 */
class HomeFragment : Fragment(R.layout.fragment_home) {
    private var binding: FragmentHomeBinding? = null
    private val model: HomeViewModel by viewModels {
        homeFactory((requireActivity().application as AquaDailyApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ui = FragmentHomeBinding.bind(view)
        binding = ui

        // Lắng nghe kết quả trả về từ AmountDialogFragment
        childFragmentManager.setFragmentResultListener(
            AmountDialogFragment.RESULT,
            viewLifecycleOwner
        ) { _, result ->
            if (result.getBoolean("is_goal")) {
                model.setGoal(result.getInt("amount"))
            } else {
                model.addDrink(result.getInt("amount"))
            }
        }

        ui.addDrink.setOnClickListener { openAmount(false, 250) }
        ui.editGoal.setOnClickListener { openAmount(true, model.state.value.goalMl) }
        ui.error.setOnClickListener { model.retry() }

        // Thu thập trạng thái từ ViewModel trong phạm vi vòng đời STARTED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect(::render)
            }
        }
    }

    /** Mở dialog nhập số lượng nước hoặc chỉnh sửa mục tiêu. */
    private fun openAmount(goal: Boolean, initial: Int) {
        val manager = childFragmentManager
        if (!manager.isStateSaved && manager.findFragmentByTag(AmountDialogFragment.TAG) == null) {
            AmountDialogFragment.newInstance(goal, initial)
                .showNow(manager, AmountDialogFragment.TAG)
        }
    }

    /** Cập nhật toàn bộ giao diện từ trạng thái [DayUiState]. */
    private fun render(state: DayUiState) {
        val ui = binding ?: return
        ui.total.text = getString(R.string.amount_ml, state.totalMl)
        ui.total.contentDescription =
            getString(R.string.progress_accessibility, state.totalMl, state.percent)
        ui.goal.text = getString(R.string.goal_label, state.goalMl)
        ui.editGoal.text = buildSpannedString {
            append(getString(R.string.remaining_title))
            append('\n')
            color(ContextCompat.getColor(requireContext(), R.color.primary)) {
                bold { append(getString(R.string.amount_ml, state.remaining)) }
            }
        }
        val remainingDescription =
            if (state.remaining == 0L) getString(R.string.goal_reached) else getString(
                R.string.remaining_label,
                state.remaining
            )
        ui.editGoal.contentDescription = getString(R.string.edit_goal) + ", " + remainingDescription
        ui.streak.text = getString(R.string.streak_short, state.streak)
        ui.streak.contentDescription = if (state.streak == 0) getString(R.string.start_streak) else getString(
            R.string.streak_label,
            state.streak
        )
        ui.progress.setProgress(state.percent)
        if (!state.loading) ui.wave.setProgress(state.totalMl, state.goalMl)
        ui.error.isVisible = state.error
        ui.loading.isVisible = state.loading || state.busy
        ui.addDrink.isEnabled = !state.busy && !state.loading
        ui.editGoal.isEnabled = !state.busy && !state.loading
    }


    override fun onResume() {
        super.onResume()
        model.refreshDay()
        binding?.wave?.setAnimating(true)
    }

    override fun onPause() {
        binding?.wave?.setAnimating(false)
        super.onPause()
    }

    override fun onDestroyView() {
        binding?.wave?.setAnimating(false)
        binding = null
        super.onDestroyView()
    }
}