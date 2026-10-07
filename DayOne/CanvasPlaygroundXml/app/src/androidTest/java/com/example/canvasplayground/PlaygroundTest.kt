package com.example.canvasplayground

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Parcel
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaygroundTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var time = 0L
    private var down = 0L
    private fun onMain(block: (LearningCanvasView) -> Unit) {
        instrumentation.runOnMainSync {
            time = SystemClock.uptimeMillis(); down = time
            val view = LearningCanvasView(instrumentation.targetContext)
            view.setPadding(24, 24, 24, 24)
            view.layout(0, 0, 1000, 1000)
            block(view)
        }
    }

    private fun send(view: View, action: Int, ids: IntArray, vararg points: Pair<Float, Float>): Boolean {
        time += 32
        val properties = Array(ids.size) { i -> MotionEvent.PointerProperties().apply {
            id = ids[i]; toolType = MotionEvent.TOOL_TYPE_FINGER
        } }
        val coords = Array(ids.size) { i -> MotionEvent.PointerCoords().apply {
            x = points[i].first; y = points[i].second; pressure = 1f; size = 1f
        } }
        val event = MotionEvent.obtain(down, time, action, ids.size, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        return try {
            if (view is TouchLessonFrame) view.dispatchTouchEvent(event) else view.onTouchEvent(event)
        } finally { event.recycle() }
    }
    private fun single(view: View, action: Int, x: Float, y: Float) =
        send(view, action, intArrayOf(7), x to y)

    @Test fun tapJitterClicksButDragDoesNot() = onMain { view ->
        var clicks = 0
        view.setOnClickListener { clicks++ }
        val x = view.centerX; val y = view.centerY
        val jitter = ViewConfiguration.get(view.context).scaledTouchSlop / 3f
        single(view, MotionEvent.ACTION_DOWN, x, y)
        single(view, MotionEvent.ACTION_MOVE, x + jitter, y)
        single(view, MotionEvent.ACTION_UP, x + jitter, y)
        assertEquals(1, clicks); assertEquals(x, view.centerX, 0.1f)
        single(view, MotionEvent.ACTION_DOWN, x, y)
        single(view, MotionEvent.ACTION_MOVE, x + 100f, y)
        assertEquals(x + 100, view.centerX, 0.1f)
        single(view, MotionEvent.ACTION_UP, x + 100, y)
        assertEquals(1, clicks)
    }

    @Test fun cancelOutsideAndDisabledDoNotClick() = onMain { view ->
        var clicks = 0
        view.setOnClickListener { clicks++ }
        assertFalse(single(view, MotionEvent.ACTION_DOWN, 0f, 0f))
        single(view, MotionEvent.ACTION_DOWN, view.centerX, view.centerY)
        single(view, MotionEvent.ACTION_CANCEL, view.centerX, view.centerY)
        assertFalse(view.isPressed)
        assertFalse(single(view, MotionEvent.ACTION_UP, view.centerX, view.centerY))
        view.isEnabled = false
        assertFalse(single(view, MotionEvent.ACTION_DOWN, view.centerX, view.centerY))
        assertFalse(view.performClick())
        assertEquals(0, clicks)
    }

    @Test fun pinchClampsScaleAndPointerHandoffDoesNotJump() = onMain { view ->
        var clicks = 0
        view.setOnClickListener { clicks++ }
        val x = view.centerX; val y = view.centerY
        single(view, MotionEvent.ACTION_DOWN, x, y)
        send(view, MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), intArrayOf(7, 19), x to y, (x + 200) to y)
        for (span in 220..1200 step 40) {
            send(view, MotionEvent.ACTION_MOVE, intArrayOf(7, 19), x to y, (x + span) to y)
        }
        assertEquals(2f, view.scaleFactor, 0.01f)
        for (span in 1100 downTo 30 step 30) {
            send(view, MotionEvent.ACTION_MOVE, intArrayOf(7, 19), x to y, (x + span) to y)
        }
        assertTrue("Pinch must shrink", view.scaleFactor < 1f)
        assertTrue("Scale must stay within limits", view.scaleFactor >= 0.5f)
        // The detector stops below its device-specific minimum span.
        view.setScale(0.01f)
        assertEquals(0.5f, view.scaleFactor, 0f)
        send(view, MotionEvent.ACTION_POINTER_UP, intArrayOf(7, 19), x to y, (x + 30) to y)
        send(view, MotionEvent.ACTION_MOVE, intArrayOf(19), (x + 80) to y)
        assertEquals(x + 50, view.centerX, 0.1f)
        send(view, MotionEvent.ACTION_UP, intArrayOf(19), (x + 80) to y)
        assertEquals(0, clicks)
    }

    @Test fun thirdPointerCanEnterAndLeaveWithoutClick() = onMain { view ->
        var clicks = 0
        view.setOnClickListener { clicks++ }
        val p = view.centerX to view.centerY
        send(view, MotionEvent.ACTION_DOWN, intArrayOf(7), p)
        send(view, MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), intArrayOf(7, 19), p, (p.first + 100) to p.second)
        send(view, MotionEvent.ACTION_POINTER_DOWN or (2 shl 8), intArrayOf(7, 19, 31),
            p, (p.first + 100) to p.second, (p.first - 100) to p.second)
        send(view, MotionEvent.ACTION_POINTER_UP or (1 shl 8), intArrayOf(7, 19, 31),
            p, (p.first + 100) to p.second, (p.first - 100) to p.second)
        send(view, MotionEvent.ACTION_POINTER_UP, intArrayOf(7, 31), p, (p.first - 100) to p.second)
        send(view, MotionEvent.ACTION_UP, intArrayOf(31), (p.first - 100) to p.second)
        assertEquals(0, clicks); assertFalse(view.isPressed)
    }

    @Test fun drawingAndTinyViewRespectPadding() = onMain { view ->
        view.moveBy(2000f, 2000f)
        assertTrue(view.centerX + view.radius() <= view.width - view.paddingRight)
        view.layout(0, 0, 80, 70)
        assertTrue(view.radius() <= 11f)
        val bitmap = Bitmap.createBitmap(80, 70, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertEquals(0, bitmap.getPixel(5, 5))
        bitmap.recycle()
        view.setPadding(100, 100, 100, 100)
        view.layout(0, 0, 40, 40)
        assertEquals(0f, view.radius(), 0f)
        assertFalse(single(view, MotionEvent.ACTION_DOWN, 20f, 20f))
    }

    @Test fun savedStateSurvivesParcelAndDifferentSize() = onMain { view ->
        view.moveBy(120f, -100f); view.setScale(1.5f)
        view.label = "Android"; view.circleColor = android.graphics.Color.RED
        val position = view.relativePosition()
        view.id = R.id.canvasView
        val hierarchy = android.util.SparseArray<android.os.Parcelable>()
        view.saveHierarchyState(hierarchy)
        val state = hierarchy[R.id.canvasView] as LearningCanvasView.SavedState
        val parcel = Parcel.obtain()
        val copy = try {
            state.writeToParcel(parcel, 0); parcel.setDataPosition(0)
            LearningCanvasView.SavedState.CREATOR.createFromParcel(parcel)
        } finally { parcel.recycle() }
        val restored = LearningCanvasView(view.context)
        restored.setPadding(24, 24, 24, 24)
        restored.layout(0, 0, 1200, 900)
        restored.id = R.id.canvasView
        val saved = android.util.SparseArray<android.os.Parcelable>()
        saved.put(R.id.canvasView, copy)
        restored.restoreHierarchyState(saved)
        assertEquals(position.first, restored.relativePosition().first, 0.001f)
        assertEquals(position.second, restored.relativePosition().second, 0.001f)
        assertEquals(1.5f, restored.scaleFactor, 0f)
        assertEquals("Android", restored.label)
        assertEquals(android.graphics.Color.RED, restored.circleColor)
    }

    @Test fun parentInterceptCancelsChild() = onMain { view ->
        val frame = TouchLessonFrame(view.context).apply { interceptDemo = true }
        frame.addView(view, ViewGroup.LayoutParams(1000, 1000))
        val exact = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        frame.measure(exact, exact); frame.layout(0, 0, 1000, 1000)
        var clicks = 0; var interceptions = 0
        view.setOnClickListener { clicks++ }; frame.onIntercepted = { interceptions++ }
        val x = view.centerX; val y = view.centerY
        single(frame, MotionEvent.ACTION_DOWN, x, y)
        assertTrue(view.isPressed)
        single(frame, MotionEvent.ACTION_MOVE, x + 150, y)
        assertFalse(view.isPressed)
        single(frame, MotionEvent.ACTION_UP, x + 150, y)
        assertEquals(x, view.centerX, 0.1f)
        assertEquals(0, clicks); assertEquals(1, interceptions)
    }

    @Test fun customViewGroupMeasuresMarginsAndIgnoresGone() = onMain { view ->
        val column = LessonColumn(view.context)
        column.setPadding(4, 5, 4, 5)
        val child = View(view.context)
        val params = ViewGroup.MarginLayoutParams(40, 30).apply {
            leftMargin = 10; rightMargin = 3; topMargin = 10; bottomMargin = 7
        }
        column.addView(child, params)
        column.addView(View(view.context).apply { visibility = View.GONE }, ViewGroup.MarginLayoutParams(300, 300))
        val limit = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)
        column.measure(limit, limit); column.layout(0, 0, column.measuredWidth, column.measuredHeight)
        assertEquals(61, column.measuredWidth); assertEquals(57, column.measuredHeight)
        assertEquals(14, child.left); assertEquals(15, child.top)
    }

    @Test fun recreateRestoresCanvasAndControlsProvideAlternatives() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var position = 0f to 0f
            scenario.onActivity { activity ->
                val canvas = activity.findViewById<LearningCanvasView>(R.id.canvasView)
                activity.findViewById<View>(R.id.dragSource2).performClick()
                activity.findViewById<View>(R.id.changeColor).performClick()
                activity.findViewById<View>(R.id.zoomIn).performClick()
                activity.findViewById<View>(R.id.moveRight).performClick()
                position = canvas.relativePosition()
                assertEquals("Android", canvas.label)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val canvas = activity.findViewById<LearningCanvasView>(R.id.canvasView)
                assertEquals("Android", canvas.label)
                assertEquals(activity.getColor(R.color.circle_purple), canvas.circleColor)
                assertEquals(1.2f, canvas.scaleFactor, 0.001f)
                assertEquals(position.first, canvas.relativePosition().first, 0.001f)
                assertTrue(canvas.performAccessibilityAction(R.id.accessibility_zoom_in, null))
                assertTrue(canvas.scaleFactor > 1.2f)
                activity.findViewById<CheckBox>(R.id.enableCanvas).isChecked = false
                assertFalse(canvas.isEnabled)
                assertFalse(activity.findViewById<View>(R.id.zoomIn).isEnabled)
                activity.findViewById<CheckBox>(R.id.enableCanvas).isChecked = true
                activity.findViewById<View>(R.id.reset).performClick()
                assertEquals(1f, canvas.scaleFactor, 0f)
                assertEquals("Intern", canvas.label)
            }
        }
    }

    @Test fun realDragCopiesTextAndCleansHighlight() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            SystemClock.sleep(800)
            fun drag(sourceId: Int, outside: Boolean = false) {
                var start = 0f to 0f
                var end = 0f to 0f
                scenario.onActivity { activity ->
                    fun center(id: Int): Pair<Float, Float> {
                        val view = activity.findViewById<View>(id)
                        val location = IntArray(2); view.getLocationOnScreen(location)
                        return (location[0] + view.width / 2f) to (location[1] + view.height / 2f)
                    }
                    start = center(sourceId)
                    end = center(if (outside) R.id.canvasView else R.id.dropTarget)
                }
                val began = SystemClock.uptimeMillis()
                val automation = instrumentation.uiAutomation
                fun inject(action: Int, point: Pair<Float, Float>) {
                    val event = MotionEvent.obtain(began, SystemClock.uptimeMillis(), action,
                        point.first, point.second, 0)
                    event.source = InputDevice.SOURCE_TOUCHSCREEN
                    try { assertTrue(automation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                inject(MotionEvent.ACTION_DOWN, start)
                // ScrollView delays pressed state; hold until the app confirms drag has started.
                val timeout = SystemClock.uptimeMillis() + 4000
                var started = false
                while (!started && SystemClock.uptimeMillis() < timeout) {
                    SystemClock.sleep(100)
                    scenario.onActivity { activity ->
                        started = activity.findViewById<TextView>(R.id.status).text.toString() ==
                            activity.getString(R.string.drag_started)
                    }
                }
                if (!started) {
                    inject(MotionEvent.ACTION_CANCEL, start)
                    fail("Long press did not start drag at $start")
                }
                for (i in 1..15) {
                    val t = i / 15f
                    inject(MotionEvent.ACTION_MOVE,
                        (start.first + (end.first - start.first) * t) to
                        (start.second + (end.second - start.second) * t))
                    SystemClock.sleep(40)
                }
                inject(MotionEvent.ACTION_UP, end)
                instrumentation.waitForIdleSync()
                SystemClock.sleep(300)
            }
            drag(R.id.dragSource2)
            scenario.onActivity { activity ->
                assertEquals("Android", activity.findViewById<LearningCanvasView>(R.id.canvasView).label)
                assertEquals("Android", activity.findViewById<TextView>(R.id.dragSource2).text.toString())
                assertEquals(1f, activity.findViewById<View>(R.id.dropTarget).alpha, 0f)
                assertEquals(activity.getString(R.string.drop_success),
                    activity.findViewById<TextView>(R.id.status).text.toString())
            }
            drag(R.id.dragSource, outside = true)
            scenario.onActivity { activity ->
                assertEquals("Android", activity.findViewById<LearningCanvasView>(R.id.canvasView).label)
                assertEquals(1f, activity.findViewById<View>(R.id.dropTarget).alpha, 0f)
            }
        }
    }
}