package com.example.aquadaily.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * ViewGroup bao bọc (Host Layout) dùng để giải quyết xung đột cử chỉ cuộn (Touch Gesture Conflict)
 * giữa một View con cuộn ngang (như Horizontal RecyclerView) và View cha cuộn ngang (như ViewPager2).
 *
 * Cơ chế hoạt động:
 * - Khi người dùng bắt đầu vuốt ngang, nếu View con vẫn còn khả năng cuộn theo hướng đó,
 *   Layout sẽ gọi `requestDisallowInterceptTouchEvent(true)` để ngăn ViewPager2 chiếm quyền điều khiển cử chỉ.
 * - Khi View con đã cuộn đến kịch biên, quyền bắt sự kiện vuốt sẽ được nhượng lại cho ViewPager2 để chuyển trang.
 */
class NestedScrollableHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val child = getChildAt(0) ?: return super.onInterceptTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                if (child.canScrollHorizontally(-1) || child.canScrollHorizontally(1)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = event.y - startY
                // ViewPager2 có ngưỡng paging slop lớn hơn RecyclerView thông thường
                if (abs(dx) * 0.5f > slop || abs(dy) > slop) {
                    val horizontal = abs(dx) * 0.5f > abs(dy)
                    val direction = if (dx > 0) -1 else 1
                    parent.requestDisallowInterceptTouchEvent(
                        horizontal && child.canScrollHorizontally(direction)
                    )
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onInterceptTouchEvent(event)
    }
}