package com.example.canvasplayground

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.hypot

/** Turn on the demo switch to observe the child receiving CANCEL in Logcat. */
class TouchLessonFrame @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var interceptDemo = false
    var onIntercepted: (() -> Unit)? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var intercepted = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        Log.d("CanvasTouch", "parent.dispatch ${MotionEvent.actionToString(event.actionMasked)}")
        if (event.actionMasked == MotionEvent.ACTION_DOWN) intercepted = false
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Log.d("CanvasTouch", "parent.intercept? ${MotionEvent.actionToString(event.actionMasked)}")
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y }
            MotionEvent.ACTION_MOVE -> if (interceptDemo && event.pointerCount == 1 &&
                hypot(event.x - downX, event.y - downY) > slop) {
                intercepted = true
                onIntercepted?.invoke()
                return true
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Explicit teaching mode lets the parent take over despite the child's request.
        if (interceptDemo && disallowIntercept) return
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // This parent consumes only drags intercepted after slop; it never detects a tap.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("CanvasTouch", "parent.touch ${MotionEvent.actionToString(event.actionMasked)}")
        return intercepted || super.onTouchEvent(event)
    }
}