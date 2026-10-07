package com.example.canvasplayground

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class LearningCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint().apply { color = context.getColor(R.color.grid); strokeWidth = dp(1f) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val icon = context.getDrawable(R.drawable.ic_triangle)?.mutate()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var baseRadius = dp(40f)
    private var grid = true
    private var visibleLabel = ""
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var hadMultiplePointers = false
    // Store relative coordinates independently of current window dimensions.
    private var fractionX = 0.5f
    private var fractionY = 0.5f

    var centerX = 0f
        private set
    var centerY = 0f
        private set
    var scaleFactor = 1f
        private set
    var onChange: (() -> Unit)? = null
    var circleColor: Int = context.getColor(R.color.circle_blue)
        set(value) {
            field = value
            circlePaint.color = value
            changed()
        }
    var label: String = context.getString(R.string.label_intern)
        set(value) {
            field = value.trim().take(24)
            refreshLabel()
            changed()
        }

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                setScale(scaleFactor * detector.scaleFactor)
                return true
            }
        }).apply {
            // This exercise uses two-finger pinch; disable one-finger quick scale.
            isQuickScaleEnabled = false
        }

    init {
        val values = context.obtainStyledAttributes(attrs, R.styleable.LearningCanvasView, defStyleAttr, 0)
        try {
            baseRadius = values.getDimension(R.styleable.LearningCanvasView_circleRadius, baseRadius).coerceAtLeast(dp(1f))
            grid = values.getBoolean(R.styleable.LearningCanvasView_showGrid, true)
            circleColor = values.getColor(R.styleable.LearningCanvasView_circleColor, circleColor)
            label = values.getString(R.styleable.LearningCanvasView_circleLabel) ?: label
        } finally { values.recycle() }
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun changed() {
        contentDescription = context.getString(R.string.canvas_description, label, (scaleFactor * 100).toInt())
        invalidate()
        onChange?.invoke()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSizeAndState(max(suggestedMinimumWidth, dp(240f).toInt() + paddingLeft + paddingRight), widthMeasureSpec, 0),
            resolveSizeAndState(max(suggestedMinimumHeight, dp(200f).toInt() + paddingTop + paddingBottom), heightMeasureSpec, 0)
        )
        Log.d("CanvasLesson", "Canvas.onMeasure: ${measuredWidth}x$measuredHeight")
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyFractions()
        keepCircleInside()
        icon?.setBounds(paddingLeft, paddingTop, paddingLeft + dp(20f).toInt(), paddingTop + dp(20f).toInt())
        refreshLabel()
        changed()
        Log.d("CanvasLesson", "Canvas.onSizeChanged: ${w}x$h")
    }

    private fun contentWidth() = (width - paddingLeft - paddingRight).coerceAtLeast(0).toFloat()
    private fun contentHeight() = (height - paddingTop - paddingBottom).coerceAtLeast(0).toFloat()
    fun radius(): Float = min(baseRadius * scaleFactor, min(contentWidth(), contentHeight()) / 2f)

    private fun applyFractions() {
        centerX = paddingLeft + contentWidth() * fractionX
        centerY = paddingTop + contentHeight() * fractionY
    }

    private fun keepCircleInside() {
        val r = radius()
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = max(left, (width - paddingRight).toFloat())
        val bottom = max(top, (height - paddingBottom).toFloat())
        centerX = centerX.coerceIn(left + r, right - r)
        centerY = centerY.coerceIn(top + r, bottom - r)
        if (contentWidth() > 0f) fractionX = ((centerX - left) / contentWidth()).coerceIn(0f, 1f)
        if (contentHeight() > 0f) fractionY = ((centerY - top) / contentHeight()).coerceIn(0f, 1f)
    }

    private fun refreshLabel() {
        textPaint.textSize = min(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics), radius() * 0.5f)
        val count = textPaint.breakText(label, true, radius() * 1.5f, null)
        visibleLabel = if (count >= label.length) label else label.take((count - 1).coerceAtLeast(0)) + "…"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d("CanvasLesson", "Canvas.onDraw")
        val checkpoint = canvas.save()
        try {
            canvas.clipRect(paddingLeft.toFloat(), paddingTop.toFloat(),
                max(paddingLeft.toFloat(), width - paddingRight.toFloat()),
                max(paddingTop.toFloat(), height - paddingBottom.toFloat()))
            if (grid) {
                var x = paddingLeft.toFloat()
                while (x < width - paddingRight) {
                    canvas.drawLine(x, paddingTop.toFloat(), x, height - paddingBottom.toFloat(), gridPaint)
                    x += dp(24f)
                }
                var y = paddingTop.toFloat()
                while (y < height - paddingBottom) {
                    canvas.drawLine(paddingLeft.toFloat(), y, width - paddingRight.toFloat(), y, gridPaint)
                    y += dp(24f)
                }
            }
            icon?.draw(canvas)
            canvas.translate(centerX, centerY)
            circlePaint.alpha = if (!isEnabled) 100 else if (isPressed) 210 else 255
            canvas.drawCircle(0f, 0f, radius(), circlePaint)
            val baseline = -(textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(visibleLabel, 0f, baseline, textPaint)
        } finally { canvas.restoreToCount(checkpoint) }
    }

    fun setScale(value: Float) {
        if (!isEnabled || !value.isFinite()) return
        scaleFactor = value.coerceIn(0.5f, 2f)
        keepCircleInside()
        refreshLabel()
        changed()
    }

    fun moveBy(dx: Float, dy: Float) {
        if (!isEnabled) return
        centerX += dx; centerY += dy
        keepCircleInside()
        changed()
    }

    fun reset() {
        finishGesture()
        fractionX = 0.5f; fractionY = 0.5f
        scaleFactor = 1f
        circleColor = context.getColor(R.color.circle_blue)
        label = context.getString(R.string.label_intern)
        applyFractions()
        keepCircleInside()
        refreshLabel()
        changed()
    }

    fun relativePosition() = fractionX to fractionY

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("CanvasTouch", "child.touch ${MotionEvent.actionToString(event.actionMasked)}")
        if (!isEnabled) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (radius() <= 0f || hypot(event.x - centerX, event.y - centerY) > radius()) return false
            activePointerId = event.getPointerId(0)
            downX = event.x; downY = event.y; lastX = downX; lastY = downY
            dragging = false; hadMultiplePointers = false; isPressed = true
            invalidate()
        } else if (activePointerId == MotionEvent.INVALID_POINTER_ID) return false

        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                hadMultiplePointers = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) { finishGesture(); return true }
                val x = event.getX(index); val y = event.getY(index)
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    if (!dragging && hypot(x - downX, y - downY) > touchSlop) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) moveBy(x - lastX, y - lastY)
                }
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val leaving = event.actionIndex
                val next = if (event.getPointerId(leaving) == activePointerId) {
                    (0 until event.pointerCount).firstOrNull { it != leaving }
                } else event.findPointerIndex(activePointerId).takeIf { it >= 0 }
                if (next == null) finishGesture() else {
                    activePointerId = event.getPointerId(next)
                    lastX = event.getX(next); lastY = event.getY(next)
                    downX = lastX; downY = lastY
                }
            }
            MotionEvent.ACTION_UP -> {
                val moved = hypot(event.x - downX, event.y - downY) > touchSlop
                val inside = hypot(event.x - centerX, event.y - centerY) <= radius()
                val click = !dragging && !hadMultiplePointers && !moved && inside
                finishGesture()
                if (click) performClick()
            }
            MotionEvent.ACTION_CANCEL -> finishGesture()
        }
        return true
    }

    private fun finishGesture() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        dragging = false; hadMultiplePointers = false; isPressed = false
        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) finishGesture()
        invalidate()
    }

    override fun performClick(): Boolean {
        if (!isEnabled) return false
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveBy(-dp(16f), 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveBy(dp(16f), 0f)
            KeyEvent.KEYCODE_DPAD_UP -> moveBy(0f, -dp(16f))
            KeyEvent.KEYCODE_DPAD_DOWN -> moveBy(0f, dp(16f))
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (isEnabled) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.accessibility_zoom_in, context.getString(R.string.zoom_in)))
            info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.accessibility_zoom_out, context.getString(R.string.zoom_out)))
            info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.accessibility_center, context.getString(R.string.center_circle)))
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        if (isEnabled) when (action) {
            R.id.accessibility_zoom_in -> { setScale(scaleFactor * 1.2f); return true }
            R.id.accessibility_zoom_out -> { setScale(scaleFactor / 1.2f); return true }
            R.id.accessibility_center -> {
                fractionX = 0.5f; fractionY = 0.5f; applyFractions(); changed(); return true
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d("CanvasLesson", "Canvas.onAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        finishGesture()
        Log.d("CanvasLesson", "Canvas.onDetachedFromWindow")
        super.onDetachedFromWindow()
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.x = fractionX; it.y = fractionY; it.scale = scaleFactor
            it.color = circleColor; it.label = label
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) { super.onRestoreInstanceState(state); return }
        super.onRestoreInstanceState(state.superState)
        fractionX = state.x.coerceIn(0f, 1f); fractionY = state.y.coerceIn(0f, 1f)
        scaleFactor = state.scale.coerceIn(0.5f, 2f)
        circleColor = state.color; label = state.label
        applyFractions(); keepCircleInside(); refreshLabel(); changed()
    }

    class SavedState : BaseSavedState {
        var x = 0.5f
        var y = 0.5f
        var scale = 1f
        var color = 0
        var label = ""
        constructor(superState: Parcelable?) : super(superState)
        private constructor(parcel: Parcel) : super(parcel) {
            x = parcel.readFloat(); y = parcel.readFloat(); scale = parcel.readFloat()
            color = parcel.readInt(); label = parcel.readString().orEmpty()
        }
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(x); out.writeFloat(y); out.writeFloat(scale)
            out.writeInt(color); out.writeString(label)
        }
        companion object {
            @JvmField val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }
}