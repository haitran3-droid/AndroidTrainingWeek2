package com.example.aquadaily

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.aquadaily.databinding.ActivityMainBinding
import com.example.aquadaily.ui.home.HomeFragment
import com.example.aquadaily.ui.infor.InforFragment
import com.example.aquadaily.ui.report.ReportFragment
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity chính duy nhất (Single Activity Architecture) của ứng dụng AquaDaily.
 *
 * Chức năng:
 * - Điều hướng giữa 3 phân hệ chính thông qua [ViewPager2] và [com.google.android.material.tabs.TabLayout]:
 *   0: Trang chủ ([HomeFragment])
 *   1: Lịch sử & Báo cáo ([ReportFragment])
 *   2: Kiến thức & Mẹo sức khỏe ([InforFragment])
 * - Hỗ trợ hiển thị tràn viền hiện đại (Edge-to-Edge) và xử lý phần đệm cho thanh hệ thống, tai thỏ và bàn phím (WindowInsets).
 * - Xử lý điều hướng thông minh khi nhấn nút Back hệ thống (tự quay về màn hình chính trước khi thoát).
 * - Đồng bộ giao diện thanh trạng thái (Status Bar) và thanh điều hướng (Navigation Bar) linh hoạt theo từng tab và chế độ sáng/tối.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mediator: TabLayoutMediator? = null

    /**
     * Callback xử lý nút Back hệ thống:
     * Khi người dùng đang ở tab khác (Report hoặc Infor), nhấn Back sẽ đưa về tab Home thay vì thoát app.
     */
    private val backToHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            goHome()
        }
    }

    /**
     * Callback theo dõi sự kiện chuyển trang trên [ViewPager2]:
     * - Bật callback nút Back nếu đang không ở tab Home (`position != 0`).
     * - Cập nhật màu nền và màu icon thanh hệ thống theo tab hiện tại.
     */
    private val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            backToHome.isEnabled = position != 0
            updateSystemBars(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kích hoạt giao diện hiển thị tràn viền (Edge-to-Edge)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSystemBars(binding.pager.currentItem)

        // Xử lý khoảng cách đệm (Padding) để nội dung không bị che khuất bởi thanh hệ thống, tai thỏ hoặc bàn phím (IME)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }

        // Thiết lập Adapter quản lý 3 Fragment trong ViewPager2
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> HomeFragment()
                1 -> ReportFragment()
                2 -> InforFragment()
                else -> error("Invalid page $position")
            }
        }

        val labels = listOf(R.string.home, R.string.report, R.string.infor)
        val icons = listOf(R.drawable.ic_water, R.drawable.ic_report, R.drawable.ic_book)

        // Kết nối TabLayout với ViewPager2 thông qua TabLayoutMediator
        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.setText(labels[position])
            tab.setIcon(icons[position])
        }.also { it.attach() }

        binding.pager.registerOnPageChangeCallback(callback)
        onBackPressedDispatcher.addCallback(this, backToHome)
        ViewCompat.requestApplyInsets(binding.root)
    }

    /** Chuyển nhanh về màn hình trang chủ (Tab 0). */
    fun goHome() {
        binding.pager.setCurrentItem(0, true)
    }

    /**
     * Tùy biến thanh hệ thống (Status Bar / Navigation Bar) và màu nền Activity:
     * - Tab Home sử dụng hình nền gradient riêng (`bg_home`).
     * - Các tab khác sử dụng màu nền tiêu chuẩn (`background`).
     * - Điều chỉnh icon Status Bar / Navigation Bar tối hoặc sáng phù hợp với nền và Dark/Light mode.
     */
    private fun updateSystemBars(position: Int) {
        val home = position == 0
        val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK !=
                Configuration.UI_MODE_NIGHT_YES
        binding.root.setBackgroundResource(if (home) R.drawable.bg_home else R.color.background)
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !home && light
            isAppearanceLightNavigationBars = light
        }
    }

    override fun onDestroy() {
        mediator?.detach()
        mediator = null
        binding.pager.unregisterOnPageChangeCallback(callback)
        binding.pager.adapter = null
        super.onDestroy()
    }
}