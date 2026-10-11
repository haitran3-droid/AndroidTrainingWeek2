package com.example.aquadaily.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.aquadaily.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * Custom View vẽ hoạt ảnh sóng nước (Water Wave Animation) phía sau thẻ trang chủ.
 *
 * Tính năng kỹ thuật & Đồ họa:
 * - Sử dụng hàm lượng giác Sin để tính toán đường cong lượn sóng gồm 2 lớp sóng (sóng trước và sóng sau).
 * - Hoạt ảnh dao động sóng liên tục với [ValueAnimator] lặp vô tận, tự động dừng/chạy theo vòng đời hiển thị của View
 *   (`onAttachedToWindow`, `onDetachedFromWindow`, `onWindowVisibilityChanged`) để tiết kiệm pin và CPU.
 * - Hoạt ảnh dâng/hạ mực nước mượt mà (650ms) bằng [android.view.animation.AccelerateDecelerateInterpolator]
 *   mỗi khi tỷ lệ phần trăm nước uống thay đổi.
 * - Vẽ thêm các hạt bong bóng nước li ti chuyển động nhẹ nhàng để tăng tính sinh động.
 */
class WaveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val backPath = Path()
    private val frontPath = Path()
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.wave_back)
    }
    private val frontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.wave_front)
        alpha = 210
    }
    private val bubblePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 155 }

    private var phase = 0f
    private var fraction = 0f
    private var targetFraction = 0f
    private var hasProgress = false
    private var levelAnimator: ValueAnimator? = null
    private var requested = false

    /** Animator tạo chu kỳ sóng dao động tuần hoàn. */
    private val animator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
        duration = 4200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    /**
     * Cập nhật mực nước theo lượng nước đã uống và mục tiêu ngày.
     *
     * @param totalMl Tổng ml đã uống.
     * @param goalMl Mục tiêu ml cần đạt.
     */
    fun setProgress(totalMl: Long, goalMl: Int) {
        val next =
            if (goalMl > 0) (totalMl.toDouble() / goalMl).coerceIn(0.0, 1.0).toFloat() else 0f
        if (hasProgress && next == targetFraction) return
        levelAnimator?.cancel()
        targetFraction = next
        if (hasProgress && canAnimate()) {
            levelAnimator = ValueAnimator.ofFloat(fraction, next).apply {
                duration = 650
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { fraction = it.animatedValue as Float; invalidate() }
                start()
            }
        } else {
            fraction = next
            invalidate()
        }
        hasProgress = true
    }

    /** Bật hoặc tắt hoạt ảnh sóng nước (thường bật khi Fragment hiển thị trên màn hình). */
    fun setAnimating(enabled: Boolean) {
        requested = enabled
        updateAnimation()
    }

    /** Cập nhật trạng thái animation dựa trên điều kiện hiển thị thực tế. */
    private fun updateAnimation() {
        val run = canAnimate()
        if (run && !animator.isStarted) animator.start()
        else if (!run && animator.isStarted) animator.cancel()
        if (!run) settleLevel()
    }

    /** Kiểm tra xem view có đủ điều kiện để chạy animation hay không. */
    private fun canAnimate() = requested && isAttachedToWindow && isShown &&
            windowVisibility == VISIBLE && ValueAnimator.areAnimatorsEnabled()

    /** Đặt ngay mức nước về giá trị mục tiêu khi không thể chạy animation. */
    private fun settleLevel() {
        levelAnimator?.cancel()
        levelAnimator = null
        fraction = targetFraction
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        settleLevel()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        )
    }

    /**
     * Dựng đường dẫn hình học (Path) cho sóng nước bằng hàm sin.
     *
     * @param path Đối tượng [Path] nhận tọa độ vẽ.
     * @param offset Độ lệch pha góc để phân biệt sóng trước và sóng sau.
     * @param base Mức sàn chiều cao của sóng.
     */
    private fun wave(path: Path, offset: Float, base: Float) {
        path.reset()
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val bottom = (height - paddingBottom).toFloat()
        val contentHeight = bottom - paddingTop
        val amplitude = minOf(12f * resources.displayMetrics.density, contentHeight * 0.035f) *
                minOf(1f, fraction / 0.1f) * (1f - fraction)
        path.moveTo(left, bottom)
        for (i in 0..40) {
            val x = left + (right - left) * i / 40f
            val y =
                paddingTop + contentHeight * base + sin(i / 40f * 2 * PI + phase + offset).toFloat() * amplitude
            path.lineTo(x, y)
        }
        path.lineTo(right, bottom)
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fraction <= 0f) return
        val checkpoint = canvas.save()
        canvas.clipRect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)

        // Vẽ 2 lớp sóng nước (sóng sau và sóng trước)
        wave(backPath, 0f, 1f - fraction)
        wave(frontPath, 2.1f, 1f - fraction)
        canvas.drawPath(backPath, backPaint)
        canvas.drawPath(frontPath, frontPaint)

        // Vẽ bong bóng nước
        val surface = paddingTop + (height - paddingTop - paddingBottom) * (1f - fraction)
        val waterDepth = height - paddingBottom - surface
        canvas.drawCircle(
            width * 0.15f,
            surface + waterDepth * 0.7f,
            2f * resources.displayMetrics.density,
            bubblePaint
        )
        canvas.drawCircle(
            width * 0.7f,
            surface + waterDepth * 0.4f,
            3f * resources.displayMetrics.density,
            bubblePaint
        )
        canvas.drawCircle(
            width * 0.88f,
            surface + waterDepth * 0.85f,
            2f * resources.displayMetrics.density,
            bubblePaint
        )
        canvas.restoreToCount(checkpoint)
    }
}