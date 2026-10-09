package com.example.fragmentexercise.ui.home

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ToggleableDecoration(
    private val delegate: RecyclerView.ItemDecoration
) : RecyclerView.ItemDecoration() {
    var enabled = true

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (enabled) {
            delegate.getItemOffsets(outRect, view, parent, state)
        } else {
            outRect.set(0, 0, 0, 0)
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (enabled) {
            delegate.onDraw(c, parent, state)
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (enabled) {
            delegate.onDrawOver(c, parent, state)
        }
    }
}