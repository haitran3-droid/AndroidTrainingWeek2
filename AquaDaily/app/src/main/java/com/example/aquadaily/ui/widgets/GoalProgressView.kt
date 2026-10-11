package com.example.aquadaily.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.aquadaily.R

/**
 * Custom View vẽ vòng tròn tiến độ hoàn thành mục tiêu nước uống hàng ngày (Circular Progress Ring).
 *
 * Chức năng:
 * - Vẽ đường tròn nền (track) và cung tròn tiến độ (arc) tương ứng với tỷ lệ phần trăm (0 - 100%).
 * - Hiển thị chữ phần trăm (% ở tâm) và chữ chú thích ("Mục tiêu" ở phía dưới tâm).
 * - Tự động co giãn kích thước chữ linh hoạt theo kích thước thực tế của View và `displayMetrics`.
 */
class GoalProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val ring = RectF()

    /** Cọ vẽ đường tròn nền mờ. */
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.progress_track)
        style = Paint.Style.STROKE
        strokeWidth = 10 * density
    }

    /** Cọ vẽ cung tròn tiến độ với nét bo tròn đầu (Round Cap). */
    private val progressPaint = Paint(track).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        strokeCap = Paint.Cap.ROUND
    }

    /** Cọ vẽ chữ phần trăm ở tâm. */
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    /** Cọ vẽ nhãn "Mục tiêu" phụ. */
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textAlign = Paint.Align.CENTER
    }

    private var percent = 0
    private var displayPercent = context.getString(R.string.progress_percent, 0)
    private val goalLabel = context.getString(R.string.progress_label)

    /** Cập nhật giá trị tiến độ mới (0..100) và vẽ lại View. */
    fun setProgress(value: Int) {
        percent = value.coerceIn(0, 100)
        displayPercent = context.getString(R.string.progress_percent, percent)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (120 * density).toInt()
        setMeasuredDimension(
            resolveSize(desired + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(desired + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val size =
            minOf(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom).coerceAtLeast(0)
        val cx = paddingLeft + (w - paddingLeft - paddingRight) / 2f
        val cy = paddingTop + (h - paddingTop - paddingBottom) / 2f
        val radius = (size / 2f - track.strokeWidth / 2f).coerceAtLeast(0f)
        ring.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Tính kích thước font chữ tương thích với kích thước của vòng tròn
        text.textSize = minOf(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                28f,
                resources.displayMetrics
            ), size * 0.23f
        )
        label.textSize = minOf(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                12f,
                resources.displayMetrics
            ), size * 0.11f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Vẽ vòng tròn nền
        canvas.drawOval(ring, track)
        // 2. Vẽ cung tròn tiến độ bắt đầu từ đỉnh 12h (-90 độ)
        canvas.drawArc(ring, -90f, 360f * percent / 100f, false, progressPaint)
        // 3. Vẽ văn bản phần trăm và nhãn
        canvas.drawText(displayPercent, ring.centerX(), ring.centerY() + text.textSize * 0.1f, text)
        canvas.drawText(goalLabel, ring.centerX(), ring.centerY() + text.textSize * 0.9f, label)
    }
}