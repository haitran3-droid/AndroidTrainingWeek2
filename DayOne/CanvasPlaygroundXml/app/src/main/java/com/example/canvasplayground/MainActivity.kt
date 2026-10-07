package com.example.canvasplayground

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var canvas: LearningCanvasView
    private lateinit var dropTarget: TextView
    private lateinit var status: TextView
    private lateinit var frame: TouchLessonFrame
    private var clicks = 0
    private var colorIndex = 0
    private var message = ""
    private val controlIds = intArrayOf(
        R.id.zoomIn, R.id.zoomOut, R.id.changeColor, R.id.reset,
        R.id.moveLeft, R.id.moveRight, R.id.moveUp, R.id.moveDown,
        R.id.dragSource, R.id.dragSource2
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        applyInsets()
        canvas = findViewById(R.id.canvasView)
        frame = findViewById(R.id.touchFrame)
        status = findViewById(R.id.status)
        dropTarget = findViewById(R.id.dropTarget)
        clicks = savedInstanceState?.getInt("clicks") ?: 0
        colorIndex = savedInstanceState?.getInt("colorIndex") ?: 0
        message = savedInstanceState?.getString("message") ?: getString(R.string.ready)
        canvas.onChange = { updateUi() }
        frame.onIntercepted = { showMessage(getString(R.string.parent_intercepted)) }
        canvas.setOnClickListener {
            clicks++
            showMessage(getString(R.string.circle_clicked))
            Toast.makeText(this, R.string.circle_clicked, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.zoomIn).setOnClickListener { canvas.setScale(canvas.scaleFactor * 1.2f) }
        findViewById<View>(R.id.zoomOut).setOnClickListener { canvas.setScale(canvas.scaleFactor / 1.2f) }
        findViewById<View>(R.id.changeColor).setOnClickListener {
            val colors = intArrayOf(R.color.circle_blue, R.color.circle_purple, R.color.circle_red)
            colorIndex = (colorIndex + 1) % colors.size
            canvas.circleColor = getColor(colors[colorIndex])
        }
        findViewById<View>(R.id.reset).setOnClickListener {
            colorIndex = 0; clicks = 0
            canvas.reset()
            showMessage(getString(R.string.ready))
        }
        val step = 16f * resources.displayMetrics.density
        findViewById<View>(R.id.moveLeft).setOnClickListener { canvas.moveBy(-step, 0f) }
        findViewById<View>(R.id.moveRight).setOnClickListener { canvas.moveBy(step, 0f) }
        findViewById<View>(R.id.moveUp).setOnClickListener { canvas.moveBy(0f, -step) }
        findViewById<View>(R.id.moveDown).setOnClickListener { canvas.moveBy(0f, step) }

        val enabled = findViewById<CheckBox>(R.id.enableCanvas)
        enabled.isChecked = savedInstanceState?.getBoolean("enabled") ?: true
        enabled.setOnCheckedChangeListener { _, checked -> setCanvasEnabled(checked) }
        val intercept = findViewById<CheckBox>(R.id.interceptDemo)
        intercept.isChecked = savedInstanceState?.getBoolean("intercept") ?: false
        frame.interceptDemo = intercept.isChecked
        intercept.setOnCheckedChangeListener { _, checked ->
            frame.interceptDemo = checked
            showMessage(getString(if (checked) R.string.intercept_on else R.string.intercept_off))
        }

        setupSource(R.id.dragSource)
        setupSource(R.id.dragSource2)
        setupDropTarget()
        setCanvasEnabled(enabled.isChecked)
        // Framework restores Custom View state after onCreate.
        canvas.post { updateUi() }
    }

    private fun setCanvasEnabled(enabled: Boolean) {
        canvas.isEnabled = enabled
        controlIds.forEach { findViewById<View>(it).isEnabled = enabled }
        updateUi()
    }

    private fun setupSource(id: Int) {
        val source = findViewById<TextView>(id)
        // A tap provides an accessible alternative to dragging.
        source.setOnClickListener {
            copyLabel(source.text.toString())
        }
        source.setOnLongClickListener { view ->
            val data = ClipData.newPlainText("lesson-label", source.text)
            val shadow = View.DragShadowBuilder(view)
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(data, shadow, null, 0)
            } else {
                @Suppress("DEPRECATION")
                view.startDrag(data, shadow, null, 0)
            }
            if (started) showMessage(getString(R.string.drag_started))
            started
        }
        source.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENDED -> {
                    showMessage(getString(if (event.result) R.string.drop_success else R.string.drop_outside))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDropTarget() {
        renderDropBackground(false)
        dropTarget.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> canvas.isEnabled &&
                    event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                DragEvent.ACTION_DRAG_ENTERED -> {
                    renderDropBackground(true); dropTarget.alpha = 0.7f; true
                }
                DragEvent.ACTION_DRAG_LOCATION -> true
                DragEvent.ACTION_DRAG_EXITED -> {
                    renderDropBackground(false); dropTarget.alpha = 1f; true
                }
                DragEvent.ACTION_DROP -> {
                    val data = event.clipData
                    if (!canvas.isEnabled || data == null || data.itemCount == 0 ||
                        !data.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) false
                    else copyLabel(data.getItemAt(0).coerceToText(this).toString())
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    renderDropBackground(false); dropTarget.alpha = 1f; true
                }
                else -> false
            }
        }
    }

    private fun copyLabel(value: String): Boolean {
        val text = value.trim().take(24)
        if (text.isEmpty() || !canvas.isEnabled) return false
        canvas.label = text
        showMessage(getString(R.string.label_copied, text))
        // Source stays intact: this is copy, not move.
        return true
    }

    private fun renderDropBackground(highlight: Boolean) {
        val density = resources.displayMetrics.density
        dropTarget.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(getColor(if (highlight) R.color.drop_active else R.color.surface))
            cornerRadius = 12f * density
            setStroke((2 * density).toInt().coerceAtLeast(1),
                getColor(if (highlight) R.color.circle_blue else R.color.outline), 6f * density, 4f * density)
        }
    }

    private fun updateUi() {
        val position = canvas.relativePosition()
        findViewById<TextView>(R.id.canvasStats).text = getString(
            R.string.canvas_stats, (canvas.scaleFactor * 100).toInt(),
            (position.first * 100).toInt(), (position.second * 100).toInt(), clicks
        )
        dropTarget.text = getString(R.string.drop_label, canvas.label)
        status.text = message
        val enabled = canvas.isEnabled
        findViewById<View>(R.id.zoomIn).isEnabled = enabled && canvas.scaleFactor < 2f
        findViewById<View>(R.id.zoomOut).isEnabled = enabled && canvas.scaleFactor > 0.5f
    }

    private fun showMessage(value: String) { message = value; updateUi() }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        Log.d("CanvasTouch", "Activity.dispatch ${MotionEvent.actionToString(event.actionMasked)}")
        return super.dispatchTouchEvent(event)
    }

    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        root.requestApplyInsets()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("clicks", clicks)
        outState.putInt("colorIndex", colorIndex)
        outState.putString("message", message)
        outState.putBoolean("enabled", canvas.isEnabled)
        outState.putBoolean("intercept", frame.interceptDemo)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        canvas.onChange = null
        frame.onIntercepted = null
        super.onDestroy()
    }
}