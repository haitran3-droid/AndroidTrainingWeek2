package com.example.canvasplayground

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/** A vertical ViewGroup that handles margins, padding, GONE, RTL and MeasureSpec. */
class LessonColumn @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {
    override fun onFinishInflate() {
        super.onFinishInflate()
        Log.d("CanvasLesson", "LessonColumn.onFinishInflate: $childCount children")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widest = 0
        var usedHeight = 0
        var state = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue
            val params = child.layoutParams as MarginLayoutParams
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, usedHeight)
            widest = max(widest, child.measuredWidth + params.leftMargin + params.rightMargin)
            usedHeight += child.measuredHeight + params.topMargin + params.bottomMargin
            state = combineMeasuredStates(state, child.measuredState)
        }
        setMeasuredDimension(
            resolveSizeAndState(max(suggestedMinimumWidth, widest + paddingLeft + paddingRight),
                widthMeasureSpec, state),
            resolveSizeAndState(max(suggestedMinimumHeight, usedHeight + paddingTop + paddingBottom),
                heightMeasureSpec, state shl MEASURED_HEIGHT_STATE_SHIFT)
        )
        Log.d("CanvasLesson", "LessonColumn.onMeasure: ${measuredWidth}x$measuredHeight")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var y = paddingTop
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val params = child.layoutParams as MarginLayoutParams
            y += params.topMargin
            val x = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                width - paddingRight - params.rightMargin - child.measuredWidth
            } else paddingLeft + params.leftMargin
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
            y += child.measuredHeight + params.bottomMargin
        }
    }

    override fun generateDefaultLayoutParams() = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(attrs: AttributeSet) = MarginLayoutParams(context, attrs)
    override fun generateLayoutParams(params: LayoutParams) = MarginLayoutParams(params)
    override fun checkLayoutParams(params: LayoutParams) = params is MarginLayoutParams
}