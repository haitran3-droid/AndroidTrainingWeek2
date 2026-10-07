# Bài giảng Android: View và ViewGroup — Level Intern

**Đối tượng:** intern đã biết Kotlin cơ bản, Activity và cách tạo layout XML.  
**Phạm vi:** Android View system, dùng Kotlin + XML.  
**Thời lượng gợi ý:** 4 buổi × 3 giờ, cộng thời gian làm bài tập.  
**Ngày đối chiếu nguồn:** 06/10/2026.

Trong tài liệu này, **multi view** được hiểu là các biến thể giao diện theo phiên bản Android, orientation (dọc/ngang), kích thước cửa sổ và cấu hình hiển thị.

Các ví dụ được viết cho bài học. Những đoạn có tên file là ví dụ để đưa vào một ứng dụng Android dùng Views; tài liệu không tự tạo ứng dụng hay cấu hình Gradle. Thay package `com.example.viewlesson` bằng package thực tế và import `R` từ namespace của ứng dụng nếu cần. Ví dụ Drag & Drop dùng API 24 trở lên.

**Trạng thái kiểm chứng:** đã kiểm tra cấu trúc Markdown và cú pháp các khối XML. Ví dụ Kotlin chưa được biên dịch hoặc chạy trên thiết bị vì workspace hiện chưa có ứng dụng Android/Gradle; dùng ma trận kiểm tra ở phần 11 khi triển khai bài thực hành.

## 1. Mục tiêu và lộ trình học

| Nội dung | Sau bài học, intern làm được |
| --- | --- |
| View, ViewGroup | Đọc cây giao diện; giải thích parent đo và bố trí child |
| Custom View | Tạo class dùng được trong XML; hỗ trợ thuộc tính tùy chỉnh |
| Vòng đời View | Phân biệt attach/detach với measure/layout/draw |
| Canvas | Vẽ hình; dùng Paint, tọa độ, padding và phép biến đổi |
| Touch | Giải thích dispatch, intercept, consume và cancel |
| Drag & Drop | Kéo dữ liệu từ nguồn đến vùng nhận, xử lý kết quả |
| Multi Touch | Theo dõi pointer ID; dùng ScaleGestureDetector |
| Nhiều cấu hình | Tạo layout theo orientation, chiều rộng, API; có fallback |
| Drawable | Viết XML và tạo Drawable bằng code; xử lý trạng thái |

### Kế hoạch giảng dạy

| Buổi | Nội dung | Hoạt động thực hành |
| --- | --- | --- |
| 1 | ViewGroup, View, vòng đời, đo kích thước | Đọc cây layout; log callback; Custom View đầu tiên |
| 2 | Canvas và Touch | Vẽ vòng tròn; click và kéo; quan sát parent intercept |
| 3 | Multi Touch và Drag & Drop | Pinch để đổi kích thước; kéo thả một nhãn |
| 4 | Resource theo cấu hình, Drawable | Layout dọc/ngang; theme sáng/tối; bài tổng hợp |

## 2. View và ViewGroup

`View` là thành phần UI có kích thước, vị trí, nội dung vẽ và khả năng nhận tương tác. `TextView`, `ImageView` là những ví dụ quen thuộc. `ViewGroup` kế thừa `View`, quản lý các View con; `LinearLayout` và `FrameLayout` là các ví dụ. [Nguồn: Custom view components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components).

```text
Activity / Window
└── LinearLayout (ViewGroup)
    ├── TextView (View)
    └── FrameLayout (ViewGroup)
        ├── ImageView (View)
        └── LearningCanvasView (Custom View)
```

### 2.1. Đọc layout trước khi viết code

| Khái niệm | Cách hiểu |
| --- | --- |
| `padding` | Khoảng trống bên trong View, cần tính khi vẽ và bố trí con |
| `layout_margin` | Khoảng trống bên ngoài child, parent xử lý nếu hỗ trợ |
| `LayoutParams` | Yêu cầu child gửi cho parent; loại params phụ thuộc parent |
| `match_parent` | Muốn dùng phần không gian parent cho phép |
| `wrap_content` | Muốn đủ chỗ cho nội dung; kết quả còn phụ thuộc MeasureSpec |
| `VISIBLE` | Có hiển thị và tham gia layout |
| `INVISIBLE` | Không hiển thị nhưng vẫn chiếm chỗ |
| `GONE` | Thường được layout chuẩn bỏ qua khi đo và bố trí |

`LinearLayout.LayoutParams` có thể chứa `weight`; `FrameLayout.LayoutParams` có `gravity`. Không gán params của một loại parent cho child thuộc loại parent khác. Custom ViewGroup phải tự quyết định xử lý margin và `GONE`. [Nguồn: ViewGroup API](https://developer.android.com/reference/android/view/ViewGroup).

**Câu hỏi trên lớp:** một `TextView` có `wrap_content` bên trong parent rộng 120dp có chắc được rộng 300dp nếu nội dung dài không? Hãy giải thích bằng ràng buộc parent.

## 3. Vòng đời View và quá trình render

### 3.1. Hai nhóm callback cần phân biệt

```text
Khởi tạo / inflate → gắn vào Window → có các lượt measure/layout/draw
                                      ↕ có thể lặp nhiều lần
                               tháo khỏi Window
```

Đây là mô hình học tập, không phải thứ tự callback cố định trong mọi tình huống. View có thể được đo nhiều lần, vẽ lại mà không đo lại, hoặc detach rồi attach lại. Vòng đời View cũng khác vòng đời Activity. [Nguồn: Custom view components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components).

| Callback | Việc nên làm |
| --- | --- |
| Constructor / `init` | Đọc attributes; tạo Paint, Path, RectF dùng lại |
| `onFinishInflate()` | Với compound view, tìm child đã inflate từ XML |
| `onAttachedToWindow()` | Đăng ký tài nguyên cần thiết khi View gắn vào Window |
| `onMeasure()` | Tính kích thước mong muốn dưới ràng buộc parent |
| `onSizeChanged()` | Cập nhật hình học khi kích thước thực tế thay đổi |
| `onLayout()` | Với ViewGroup, đặt vị trí các child |
| `onDraw()` | Vẽ nội dung của View |
| `onDetachedFromWindow()` | Gỡ listener, hủy công việc gắn với việc attach |

Attach không bảo đảm View đang hiện trên màn hình. Khi dùng animation hoặc tác vụ liên tục, cần xét thêm visibility và lifecycle của nơi chứa View.

### 3.2. Measure → Layout → Draw

Parent truyền ràng buộc đo xuống child. Child trả về `measuredWidth`, `measuredHeight`; parent đặt các cạnh của child trong bước layout. `width`, `height` phản ánh kích thước sau layout. Không lấy kích thước cuối cùng trong constructor. [Nguồn: How Android draws views](https://developer.android.com/guide/topics/ui/how-android-draws).

| MeasureSpec mode | Ý nghĩa | Ví dụ dễ nhớ |
| --- | --- | --- |
| `EXACTLY` | Parent yêu cầu kích thước cụ thể | Phải rộng 200px |
| `AT_MOST` | Có giới hạn trên | Được rộng tối đa 200px |
| `UNSPECIFIED` | Spec này không áp giới hạn kích thước | Tự tính theo nội dung |

Dùng `MeasureSpec.getMode()` và `getSize()` để đọc spec. Khi override `onMeasure()`, phải gọi `setMeasuredDimension()`; dùng `resolveSizeAndState()` để tôn trọng spec. [Nguồn: MeasureSpec API](https://developer.android.com/reference/android/view/View.MeasureSpec), [Custom drawing](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing).

**ViewGroup tùy chỉnh xếp dọc:** trong `onMeasure()`, đo từng child bằng spec phù hợp, lấy chiều rộng lớn nhất và cộng chiều cao; tính padding/margin; ghép measured state rồi resolve theo spec parent. Trong `onLayout()`, đi từ trên xuống và gọi `child.layout(left, top, right, bottom)`. Nếu hỗ trợ margin, cần `MarginLayoutParams` và cách đo tương ứng. [Nguồn: ViewGroup API](https://developer.android.com/reference/android/view/ViewGroup).

### 3.3. Khi nào invalidate, khi nào requestLayout?

| Thay đổi | Cách xử lý thường dùng |
| --- | --- |
| Đổi màu, điểm vẽ trong cùng vùng kích thước | `invalidate()` |
| Đổi nội dung làm kích thước mong muốn thay đổi | `requestLayout()` và `invalidate()` |
| Cập nhật hình vẽ theo frame animation | `postInvalidateOnAnimation()` |

`invalidate()` yêu cầu vẽ lại ở lượt phù hợp, không gọi `onDraw()` ngay lập tức. `requestLayout()` yêu cầu một lượt layout. Tránh gọi chúng liên tục trong `onDraw()`. [Nguồn: Create a view class](https://developer.android.com/develop/ui/views/layout/custom-views/create-view), [Optimize a custom view](https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view).

**Bài tập:** log `onAttachedToWindow`, `onMeasure`, `onSizeChanged`, `onDraw`, `onDetachedFromWindow`. Đổi màu rồi đổi kích thước; so sánh log. Không kết luận mọi callback luôn chạy đúng một lần.

## 4. Custom View và Canvas

### 4.1. Chọn cách tạo Custom View

| Nhu cầu | Cách làm |
| --- | --- |
| Thêm hành vi cho widget có sẵn | Kế thừa widget tương ứng |
| Ghép ảnh, nhãn, nút thành component dùng lại | Compound view, kế thừa một ViewGroup |
| Vẽ biểu đồ hoặc hình học riêng | Kế thừa `View`, override `onDraw()` |
| Quy tắc bố trí child riêng | Kế thừa `ViewGroup`, xử lý đo và layout |

Custom attributes khai báo trong `res/values/attrs.xml`; đọc bằng `obtainStyledAttributes()` và luôn `recycle()` trong `finally`. Constructor nhận `Context, AttributeSet` cho phép inflate từ XML. [Nguồn: Create a view class](https://developer.android.com/develop/ui/views/layout/custom-views/create-view).

### 4.2. Canvas và Paint

`Canvas` cung cấp thao tác vẽ; `Paint` mô tả màu, kiểu nét và cách hiển thị. Tọa độ trong View thường có gốc ở góc trên trái, X tăng sang phải, Y tăng xuống dưới. Canvas dùng pixel; kích thước thiết kế dùng dp, cỡ chữ dùng sp và chuyển sang pixel khi vẽ. [Nguồn: Custom drawing](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing).

| API | Bài tập áp dụng |
| --- | --- |
| `drawCircle()` | Vẽ nút tròn hoặc điểm điều khiển |
| `drawRect()`, `drawRoundRect()` | Vẽ nền thẻ |
| `drawLine()`, `drawPath()` | Vẽ trục, đường biểu diễn |
| `drawText()` | Vẽ nhãn; chú ý Y là baseline |
| `drawBitmap()` | Vẽ ảnh vào vùng đích |
| `save()`, `translate()`, `scale()`, `restoreToCount()` | Biến đổi một nhóm hình |

Khi căn chữ theo chiều dọc, có thể dùng `baseline = centerY - (paint.ascent() + paint.descent()) / 2f`. Các phép biến đổi Canvas ảnh hưởng những lệnh vẽ tiếp theo; cần save/restore để giới hạn phạm vi. [Nguồn: Drawables overview](https://developer.android.com/develop/ui/views/graphics/drawables).

```kotlin
// Đặt trong onDraw(canvas) của một View; paint đã được tạo ở ngoài.
val checkpoint = canvas.save()
try {
    canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
    canvas.drawLine(0f, 0f, 80f, 80f, paint) // Các giá trị ở đây là px.
} finally {
    canvas.restoreToCount(checkpoint)
}
```

Tạo và dùng lại Paint/Path thay vì cấp phát mỗi frame. Tránh đọc file, gọi mạng hay tính toán nặng trong `onDraw()`. Một ViewGroup không cần vẽ nền riêng có thể bỏ qua `onDraw()`; khi cần vẽ nội dung của nó, xem xét `setWillNotDraw(false)`. [Nguồn: Optimize a custom view](https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view), [ViewGroup API](https://developer.android.com/reference/android/view/ViewGroup).

## 5. Flow của Touch

### 5.1. Một gesture là một chuỗi sự kiện

```text
Một ngón: DOWN → MOVE → MOVE → UP
Bị hủy:  DOWN → MOVE → CANCEL
Hai ngón: DOWN → POINTER_DOWN → MOVE → POINTER_UP → UP
```

`ACTION_DOWN` bắt đầu gesture; `ACTION_UP` kết thúc bằng ngón cuối rời màn hình. `ACTION_CANCEL` yêu cầu dừng thao tác đang diễn ra, không thực hiện click/drop thành công. [Nguồn: Multi-touch gestures](https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi), [Touch in ViewGroup](https://developer.android.com/develop/ui/views/touch-and-input/gestures/viewgroup).

### 5.2. Đường đi thường gặp

```text
Activity.dispatchTouchEvent
  → Window / DecorView
    → Parent.dispatchTouchEvent
      → Parent.onInterceptTouchEvent
        ├── không chặn → Child.dispatchTouchEvent
        │                → OnTouchListener nếu đủ điều kiện
        │                → Child.onTouchEvent nếu listener chưa consume
        └── chặn      → Parent.onTouchEvent
```

Sơ đồ được đơn giản hóa cho cây parent/child. Không phải mọi node đều nhận đủ mọi callback ở mọi event. `dispatchTouchEvent()` phân phối; `onInterceptTouchEvent()` quyết định parent có lấy gesture từ child; `onTouchEvent()` xử lý. [Nguồn: Touch in ViewGroup](https://developer.android.com/develop/ui/views/touch-and-input/gestures/viewgroup).

| Nơi trả về boolean | `true` có nghĩa gì? |
| --- | --- |
| `onInterceptTouchEvent()` | Parent lấy quyền xử lý, chặn gửi tiếp xuống child |
| `OnTouchListener.onTouch()` | Listener đã consume; thường không gọi tiếp `onTouchEvent()` |
| `onTouchEvent()` | View đã xử lý event |
| `dispatchTouchEvent()` | Event đã được xử lý qua đường dispatch này |

Nếu child không nhận xử lý `DOWN`, nó thường không trở thành touch target cho phần còn lại của gesture. Khi parent intercept giữa gesture, child đang xử lý nhận `CANCEL` và cần xóa trạng thái kéo/pressed. [Nguồn: Make a custom view interactive](https://developer.android.com/develop/ui/views/layout/custom-views/making-interactive), [Touch in ViewGroup](https://developer.android.com/develop/ui/views/touch-and-input/gestures/viewgroup).

### 5.3. Click, drag và phối hợp parent

Một tap thường di chuyển ít. Dùng `ViewConfiguration.scaledTouchSlop` làm ngưỡng bắt đầu kéo; không coi mọi `MOVE` là drag. `event.x/y` thuộc hệ tọa độ View nhận event; `rawX/rawY` thuộc hệ tọa độ màn hình. Tránh trộn chúng trong cùng phép tính. [Nguồn: Drag and scale](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale), [Touch in ViewGroup](https://developer.android.com/develop/ui/views/touch-and-input/gestures/viewgroup).

Child có thể gọi `parent.requestDisallowInterceptTouchEvent(true)` khi cần giữ gesture, rồi trả lại ở `UP/CANCEL`. Hãy quyết định rõ ưu tiên khi vùng kéo nằm trong container cuộn; không chặn mọi gesture của parent một cách máy móc.

Khi tự nhận biết tap trong Custom View, gọi `performClick()` để tích hợp click listener và accessibility. Các thao tác chỉ có thể kéo/pinch cũng nên có nút hoặc hành động tương đương cho người dùng công nghệ hỗ trợ. [Nguồn: Make a custom view interactive](https://developer.android.com/develop/ui/views/layout/custom-views/making-interactive).

**Thực hành flow:** tạo parent và child có log dispatch/intercept/touch, gọi `super` để giữ hành vi mặc định. Cho parent intercept sau khi kéo vượt slop. Giải thích vì sao child nhận `CANCEL`, rồi thử giữ quyền xử lý từ child.

## 6. Multi Touch

### 6.1. Pointer ID khác pointer index

| Khái niệm | Vai trò |
| --- | --- |
| `pointerCount` | Số pointer trong event hiện tại |
| `getPointerId(index)` | Lấy ID của pointer ở index đó |
| `findPointerIndex(id)` | Tìm index hiện tại cho ID đã lưu |
| `actionMasked` | Loại action, không chứa phần mã hóa index |
| `actionIndex` | Pointer đổi trạng thái ở DOWN/UP của pointer |

ID ổn định trong thời gian pointer còn hoạt động; index có thể đổi giữa các event. Vì vậy lưu **ID**, tra lại index mỗi `MOVE`; xử lý trường hợp index bằng `-1`. Khi ngón đang kéo rời màn hình, chọn pointer còn lại và cập nhật mốc tọa độ để hình không nhảy. [Nguồn: Multi-touch gestures](https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi).

Ví dụ: event trước có index 0 → ID 7, index 1 → ID 12. Sau khi ID 7 rời màn hình, event tiếp theo có thể có index 0 → ID 12. Giả định “index 0 luôn là cùng một ngón” sẽ sai.

### 6.2. Pinch bằng ScaleGestureDetector

Gửi chuỗi touch cho `ScaleGestureDetector`; trong `onScale()`, nhân hệ số hiện tại với `detector.scaleFactor`, giới hạn khoảng hợp lý và yêu cầu vẽ lại. Ví dụ phần 7 phóng vòng tròn quanh tâm của nó. Với ảnh, có thể biến đổi Canvas/Matrix quanh `focusX/focusY` để zoom tại vị trí hai ngón. [Nguồn: Drag and scale](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale).

**Không nhầm:** Multi Touch là nhiều pointer trong một gesture; nhiều layout theo orientation/API là lựa chọn resource theo cấu hình.

## 7. Ví dụ xuyên suốt: vẽ, click, kéo và pinch vòng tròn

### 7.1. Attributes

File `app/src/main/res/values/attrs.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <declare-styleable name="LearningCanvasView">
        <attr name="circleColor" format="color" />
    </declare-styleable>
</resources>
```

### 7.2. Custom View hoàn chỉnh

File `app/src/main/java/com/example/viewlesson/LearningCanvasView.kt`:

```kotlin
package com.example.viewlesson

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class LearningCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val baseRadius = dp(32f)
    private var scaleFactor = 1f
    private var centerX = 0f
    private var centerY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var hadMultiplePointers = false

    var circleColor: Int = Color.rgb(37, 99, 235)
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor = (scaleFactor * detector.scaleFactor)
                    .coerceIn(0.5f, 2f)
                keepCircleInside()
                invalidate()
                return true
            }
        }
    )

    init {
        val values = context.obtainStyledAttributes(
            attrs, R.styleable.LearningCanvasView, defStyleAttr, 0
        )
        try {
            circleColor = values.getColor(
                R.styleable.LearningCanvasView_circleColor, circleColor
            )
        } finally {
            values.recycle()
        }
        isClickable = true
        isFocusable = true
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = max(
            suggestedMinimumWidth,
            dp(240f).toInt() + paddingLeft + paddingRight
        )
        val desiredHeight = max(
            suggestedMinimumHeight,
            dp(200f).toInt() + paddingTop + paddingBottom
        )
        setMeasuredDimension(
            resolveSizeAndState(desiredWidth, widthMeasureSpec, 0),
            resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = (paddingLeft + w - paddingRight) / 2f
        centerY = (paddingTop + h - paddingBottom) / 2f
        keepCircleInside()
    }

    private fun radius(): Float {
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val contentHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        return min(baseRadius * scaleFactor, min(contentWidth, contentHeight) / 2f)
    }

    private fun keepCircleInside() {
        val r = radius()
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = max(left, (width - paddingRight).toFloat())
        val bottom = max(top, (height - paddingBottom).toFloat())
        centerX = centerX.coerceIn(left + r, right - r)
        centerY = centerY.coerceIn(top + r, bottom - r)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, radius(), paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (radius() <= 0f ||
                hypot(event.x - centerX, event.y - centerY) > radius()
            ) return false
            activePointerId = event.getPointerId(0)
            downX = event.x
            downY = event.y
            lastX = downX
            lastY = downY
            dragging = false
            hadMultiplePointers = false
            isPressed = true
        } else if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
            return false
        }

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                hadMultiplePointers = true
                // Bài học ưu tiên pinch khi có thêm pointer trong vùng View.
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) {
                    finishGesture()
                    return true
                }
                val x = event.getX(index)
                val y = event.getY(index)
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    if (!dragging && hypot(x - downX, y - downY) > touchSlop) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (dragging) {
                        centerX += x - lastX
                        centerY += y - lastY
                        keepCircleInside()
                        invalidate()
                    }
                }
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val leaving = event.actionIndex
                val leavingId = event.getPointerId(leaving)
                val nextIndex = if (leavingId == activePointerId) {
                    (0 until event.pointerCount).firstOrNull { it != leaving }
                } else {
                    event.findPointerIndex(activePointerId).takeIf { it >= 0 }
                }
                if (nextIndex == null) {
                    finishGesture()
                } else {
                    activePointerId = event.getPointerId(nextIndex)
                    lastX = event.getX(nextIndex)
                    lastY = event.getY(nextIndex)
                    downX = lastX
                    downY = lastY
                }
            }
            MotionEvent.ACTION_UP -> {
                val moved = hypot(event.x - downX, event.y - downY) > touchSlop
                val inside = hypot(event.x - centerX, event.y - centerY) <= radius()
                val shouldClick = !dragging && !hadMultiplePointers && !moved && inside
                finishGesture()
                if (shouldClick) performClick()
            }
            MotionEvent.ACTION_CANCEL -> finishGesture()
        }
        return true
    }

    private fun finishGesture() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        dragging = false
        hadMultiplePointers = false
        isPressed = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
```

### 7.3. Dùng trong XML và Activity

File `app/src/main/res/layout/activity_canvas.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.example.viewlesson.LearningCanvasView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/canvasView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp"
    android:contentDescription="@string/canvas_description"
    app:circleColor="#2563EB" />
```

Thêm vào `res/values/strings.xml`:

```xml
<resources>
    <string name="canvas_description">Vùng tương tác vòng tròn</string>
    <string name="circle_clicked">Đã chạm vào vòng tròn</string>
</resources>
```

Đặt trong `Activity.onCreate()` sau `super.onCreate()`:

```kotlin
setContentView(R.layout.activity_canvas)
findViewById<LearningCanvasView>(R.id.canvasView).setOnClickListener {
    android.widget.Toast.makeText(
        this, R.string.circle_clicked, android.widget.Toast.LENGTH_SHORT
    ).show()
}
```

### 7.4. Đọc và kiểm chứng ví dụ

1. Chỉ đọc constructor, `onMeasure()`, `onSizeChanged()`, `onDraw()` trước.
2. Theo dõi `DOWN → MOVE → UP`; chỉ kéo sau khi vượt slop.
3. Thêm ngón thứ hai; giải thích detector và `hadMultiplePointers`.
4. Nhấc ngón đang kéo; kiểm tra hình không nhảy sang vị trí ngón khác.
5. Giảm vùng View hoặc tăng padding; vòng tròn phải nằm trong vùng nội dung.
6. Cho parent intercept; `CANCEL` phải xóa trạng thái và không gọi click.

**Giới hạn bài mẫu:** vị trí được đưa về tâm khi size thay đổi; chưa lưu trạng thái qua việc tạo lại Activity. Nếu parent cuộn lấy gesture trước khi child xin giữ quyền, child sẽ bị cancel — đây là tình huống dùng để học intercept. Ví dụ pinch đổi bán kính quanh tâm, chưa zoom quanh điểm focus; chưa có thao tác thay thế kéo/pinch cho accessibility. Các yêu cầu này được đưa vào bài tổng hợp.

## 8. Touch drag và Drag & Drop

### 8.1. Chọn đúng cơ chế

| Cơ chế | Ví dụ | Dữ liệu chính |
| --- | --- | --- |
| Kéo bằng Touch | Di chuyển vòng tròn trên Canvas | MotionEvent và tọa độ |
| Android Drag & Drop | Kéo nhãn từ thẻ sang vùng nhận | ClipData, DragEvent, drag shadow |
| Multi Touch | Hai ngón phóng hình | Nhiều pointer, scale factor |

Kéo một hình trong Canvas không tự tạo Drag & Drop của Android. Drag & Drop có nguồn, dữ liệu, bóng kéo và vùng nhận. `startDragAndDrop()` có từ API 24; khi hỗ trợ API thấp hơn cần nhánh tương thích hoặc AndroidX phù hợp. [Nguồn: Implement drag and drop with views](https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/view).

### 8.2. Các trạng thái DragEvent

| Action | Việc cần xử lý |
| --- | --- |
| `ACTION_DRAG_STARTED` | Kiểm tra MIME type, trả `true` nếu có thể nhận |
| `ACTION_DRAG_ENTERED` | Highlight vùng nhận |
| `ACTION_DRAG_LOCATION` | Cập nhật vị trí nếu cần |
| `ACTION_DRAG_EXITED` | Xóa highlight |
| `ACTION_DROP` | Đọc dữ liệu, thực hiện thao tác; trả kết quả |
| `ACTION_DRAG_ENDED` | Cleanup kể cả khi không drop thành công |

Chỉ đọc `clipData` ở `DROP`; `result` có ý nghĩa ở `DRAG_ENDED`. Không coi `ENTERED` là đã nhận dữ liệu. [Nguồn: DragEvent API](https://developer.android.com/reference/android/view/DragEvent).

### 8.3. Ví dụ kéo text trong cùng Activity

Tạo layout có hai `TextView` ID `dragSource`, `dropTarget`. Đặt đoạn dưới trong `onCreate()` sau `setContentView()`; dùng **minSdk ≥ 24** cho bài này. Import `ClipData`, `ClipDescription` từ `android.content`; `DragEvent`, `View` từ `android.view`; `TextView` từ `android.widget`.

```kotlin
val source = findViewById<TextView>(R.id.dragSource)
val target = findViewById<TextView>(R.id.dropTarget)

source.setOnLongClickListener { view ->
    val data = ClipData.newPlainText("lesson-label", source.text)
    view.startDragAndDrop(data, View.DragShadowBuilder(view), null, 0)
}

target.setOnDragListener { view, event ->
    when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> {
            event.clipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                == true
        }
        DragEvent.ACTION_DRAG_ENTERED -> {
            view.alpha = 0.6f
            true
        }
        DragEvent.ACTION_DRAG_LOCATION -> true
        DragEvent.ACTION_DRAG_EXITED -> {
            view.alpha = 1f
            true
        }
        DragEvent.ACTION_DROP -> {
            val data = event.clipData
            if (data == null || data.itemCount == 0) {
                false
            } else {
                target.text = data.getItemAt(0).coerceToText(view.context)
                true
            }
        }
        DragEvent.ACTION_DRAG_ENDED -> {
            view.alpha = 1f
            // event.result cho biết có vùng nhận xử lý drop thành công hay không.
            true
        }
        else -> false
    }
}
```

Ví dụ này **copy** text, chưa di chuyển View nguồn. Nếu làm thao tác move, chỉ cập nhật/xóa nguồn sau khi đã xác nhận thành công. URI giữa ứng dụng còn cần xử lý quyền truy cập; nằm ngoài bài thực hành đầu tiên.

**Bài tập:** thử thả vào vùng nhận, thả bên ngoài và kéo dữ liệu có MIME type khác. Mọi trường hợp kết thúc phải trả alpha về 1.

## 9. Multi view: resource theo cấu hình

### 9.1. Một tên resource, nhiều biến thể

```text
app/src/main/res/
├── layout/activity_canvas.xml             # Mặc định
├── layout-land/activity_canvas.xml        # Ngang
├── layout-w600dp/activity_canvas.xml      # Cửa sổ đủ rộng
├── layout-w600dp-land/activity_canvas.xml # Kết hợp điều kiện
├── drawable/bg_action.xml                 # Fallback
├── drawable-v21/bg_action.xml             # API 21 trở lên
├── values/colors.xml                      # Màu mặc định
└── values-night/colors.xml                # Màu dark mode
```

Code vẫn gọi `R.layout.activity_canvas`. Android chọn resource theo cấu hình; không cần tự viết `if (orientation)` để chọn layout trong ví dụ này. Các biến thể phải giữ ID và kiểu View tương thích với code chung. Luôn cung cấp fallback cho layout và tài nguyên cần thiết. [Nguồn: App resources overview (Views)](https://developer.android.com/topic/architecture/views/resources/providing-resources-views).

| Qualifier | Dùng khi |
| --- | --- |
| `land`, `port` | Cần bố cục ngang/dọc khác nhau |
| `w600dp` | Chiều rộng khả dụng ít nhất 600dp |
| `sw600dp` | Smallest width của cấu hình ít nhất 600dp |
| `v21`, `v24` | Resource dùng từ API đó trở lên |
| `night` | Cấu hình dark mode |
| `hdpi`, `xhdpi`… | Bitmap theo density |
| `nodpi` | Bitmap không tự scale theo density |

`w600dp` phù hợp với bố cục cần đủ chiều rộng hiện tại; `sw600dp` có ý nghĩa khác, không phải phép kiểm tra “thiết bị là tablet”. `v24` nghĩa là API ≥ 24, không phải versionName của app. [Nguồn: App resources overview (Views)](https://developer.android.com/topic/architecture/views/resources/providing-resources-views).

### 9.2. Quy tắc chọn resource

Android loại các biến thể không phù hợp rồi chọn theo độ ưu tiên qualifier. **Không** chọn đơn giản bằng “folder có nhiều qualifier nhất”. Qualifier phải đúng thứ tự; ví dụ `layout-w600dp-land-v24` hợp lệ. Resource `vNN` chỉ giới hạn resource, không làm lời gọi Kotlin API mới tự chạy được trên Android cũ. [Nguồn: App resources overview](https://developer.android.com/guide/topics/resources/providing-resources).

Ví dụ chỉ có `layout/` và `layout-land/`: dọc dùng mặc định, ngang dùng `land`. Nếu thêm `layout-w600dp/`, trên cửa sổ ngang đủ rộng, qualifier width có độ ưu tiên cao hơn orientation. Muốn bố cục riêng cho tổ hợp đó, thêm `layout-w600dp-land/`.

### 9.3. Thực hành nhiều layout

1. Trong layout mặc định, dùng `LinearLayout` dọc: vùng Canvas phía trên, các nút phía dưới.
2. Trong `layout-land`, bố trí ngang: Canvas bên trái, các nút bên phải.
3. Trong `layout-w600dp`, dành thêm chỗ cho phần hướng dẫn.
4. Giữ ID `canvasView` và các nút chung, cùng loại View ở những layout đó.
5. Đổi xoay, kích thước cửa sổ và dark mode; quan sát layout được chọn.

Để minh họa weight trong `LinearLayout`, vùng Canvas có thể dùng `layout_height="0dp"`, `layout_weight="1"` khi xếp dọc; đổi sang `layout_width="0dp"` khi xếp ngang. Weight là quy tắc của LinearLayout, không áp dụng nguyên xi cho parent khác.

### 9.4. Configuration change và state

Thay đổi cấu hình thường tạo lại Activity. Không thêm `android:configChanges` chỉ để che việc mất state; nếu tự xử lý, ứng dụng phải cập nhật tài nguyên và UI tương ứng. [Nguồn: Handle configuration changes](https://developer.android.com/guide/topics/resources/runtime-changes).

Với bài tổng hợp, lưu tâm hình dưới dạng tỷ lệ trong vùng nội dung và lưu scale. Có thể dùng `onSaveInstanceState()`/`onRestoreInstanceState()` cùng `View.BaseSavedState`; View cần ID ổn định để framework khôi phục state. Dữ liệu nghiệp vụ nên do tầng state của màn hình quản lý. [Nguồn: Custom view components](https://developer.android.com/develop/ui/views/layout/custom-views/custom-components).

## 10. Drawable bằng XML và code

### 10.1. Drawable là gì?

Drawable là đối tượng có thể vẽ lên Canvas; không phải một View và không có cây child hay flow touch riêng như View. Có thể dùng làm background, nội dung ImageView hoặc tự vẽ bằng `drawable.draw(canvas)`. [Nguồn: Drawables overview](https://developer.android.com/develop/ui/views/graphics/drawables).

| Loại | Nhu cầu điển hình |
| --- | --- |
| `shape` | Nền màu/gradient, bo góc, viền |
| `selector` | Nền thay đổi theo pressed, enabled, selected |
| `layer-list` | Chồng nhiều lớp hình |
| `ripple` | Phản hồi chạm, framework từ API 21 |
| `vector` | Icon mô tả bằng đường vector, framework từ API 21 |
| `inset` | Bọc Drawable với khoảng lùi |
| `clip`, `level-list` | Hiển thị theo level, ví dụ tiến độ |
| `bitmap`, nine-patch | Ảnh và ảnh có vùng co giãn |

[Nguồn: Drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).

### 10.2. Shape: nền bo góc

File `res/drawable/bg_card.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#EFF6FF" />
    <corners android:radius="12dp" />
    <stroke android:width="1dp" android:color="#2563EB" />
</shape>
```

Gán bằng `android:background="@drawable/bg_card"`. Màu hex được dùng để ví dụ độc lập; bài tổng hợp cần chuyển sang `@color/...` và có biến thể `values-night`.

### 10.3. Selector: trạng thái disabled/pressed/default

File `res/drawable/bg_action.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false">
        <shape android:shape="rectangle">
            <solid android:color="#CBD5E1" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#1D4ED8" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item android:drawable="@drawable/bg_card" />
</selector>
```

Selector chọn **item khớp đầu tiên**, nên default đặt cuối. `selected`, `activated`, `pressed` là các trạng thái khác nhau; đặt đúng state trên View. Selector màu trong `res/color` tạo `ColorStateList`, khác selector Drawable trong `res/drawable`. [Nguồn: Drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).

### 10.4. Ripple và Vector

File `res/drawable-v21/bg_action.xml`, dùng cùng tên fallback ở trên:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#402563EB">
    <item android:drawable="@drawable/bg_card" />
    <item android:id="@android:id/mask">
        <shape android:shape="rectangle">
            <solid android:color="#FFFFFF" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</ripple>
```

View cần nhận trạng thái chạm, ví dụ `clickable="true"`. Biến thể ripple này chưa có nền disabled riêng; đó là bài nâng cấp selector bên trong ripple.

File `res/drawable/ic_triangle.xml` (bài ví dụ dùng API ≥ 21):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#2563EB" android:pathData="M12,3 L22,21 L2,21 Z" />
</vector>
```

`width/height` là kích thước hiển thị nội tại; viewport là hệ tọa độ của path. Nếu hỗ trợ Android thấp hơn API 21, dùng cơ chế vector tương thích phù hợp với widget và dependency của dự án. [Nguồn: Drawable resources](https://developer.android.com/guide/topics/resources/drawable-resource).

### 10.5. Tạo GradientDrawable bằng code

Đặt sau khi lấy được View `target`; import `Color`, `GradientDrawable`:

```kotlin
val density = target.resources.displayMetrics.density
val background = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setColor(android.graphics.Color.rgb(239, 246, 255))
    cornerRadius = 12f * density
    setStroke(
        (1f * density).toInt().coerceAtLeast(1),
        android.graphics.Color.rgb(37, 99, 235)
    )
}
target.background = background
```

XML `<shape>` thường được inflate thành `GradientDrawable`. Với code, bán kính và độ rộng viền dùng px; XML hỗ trợ dp. Dùng code khi tham số phụ thuộc dữ liệu runtime, dùng XML khi cấu hình tĩnh có thể quản lý bằng resource. [Nguồn: GradientDrawable API](https://developer.android.com/reference/android/graphics/drawable/GradientDrawable).

### 10.6. Drawable dùng lại và tự vẽ

Drawable lấy từ cùng resource có thể chia sẻ state. Gọi `mutate()` trước khi sửa riêng một instance; không gán cùng một instance cho nhiều View host. Khi tự vẽ, đặt `bounds` theo vùng đích rồi gọi `draw(canvas)`. [Nguồn: Drawable API](https://developer.android.com/reference/android/graphics/drawable/Drawable).

```kotlin
// Trong một Custom View; ví dụ này dùng API >= 21.
private val icon = context.getDrawable(R.drawable.ic_triangle)?.mutate()

override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    icon?.setBounds(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
}

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    icon?.draw(canvas)
}
```

Đây là snippet cho **một Custom View khác**, không ghép thêm các override trùng tên vào class phần 7. Drawable tự vẽ có state/animation còn cần nối state, callback và cơ chế invalidate của host; đặt làm background để framework quản lý thường đơn giản hơn.

## 11. Bài tập và bài tổng hợp

### 11.1. Bài tập từng phần

| Bài | Yêu cầu | Tiêu chí đạt |
| --- | --- | --- |
| A — Cây View | Vẽ cây UI của một màn hình; chỉ ra LayoutParams | Giải thích đúng vai trò parent và child |
| B — Custom View | Vẽ thẻ có viền, nhãn; hỗ trợ màu từ XML | wrap_content đúng, tính padding, đổi màu runtime |
| C — Touch | Click và kéo vòng tròn | Vượt slop mới drag; CANCEL không click |
| D — Multi Touch | Pinch và đổi active pointer | Không nhảy tọa độ; không click sau pinch |
| E — Drag & Drop | Copy nhãn sang vùng nhận | Chấp nhận đúng MIME; cleanup khi thả ngoài |
| F — Resource | Layout mặc định/ngang/rộng; màu ngày/đêm | ID tương thích; chọn đúng biến thể |
| G — Drawable | shape, selector, ripple, vector và code | Phản hồi đúng state; đổi riêng một Drawable |

### 11.2. Bài tổng hợp: Canvas Playground

Tạo màn hình có vùng Canvas, nhãn kéo thả và các nút đổi màu/reset/phóng to/thu nhỏ. Các nút dùng Drawable và cung cấp cách thao tác thay thế pinch cho accessibility.

Yêu cầu nghiệm thu:

- Custom View có attributes, đo kích thước hợp lý và không vẽ đè padding.
- Tap gọi click; kéo không kích hoạt click; cancel không hoàn tất thao tác.
- Pinch có giới hạn scale, đổi pointer không gây nhảy hình.
- Drag & Drop nhận đúng dữ liệu; phân biệt copy với move.
- Có layout dọc, ngang và cửa sổ rộng; dùng chung code truy cập View.
- Có Drawable XML, Drawable tạo bằng code và màu cho dark mode.
- Sau xoay hoặc tạo lại màn hình, phục hồi vị trí tương đối và scale.
- Có nhãn và thao tác accessibility phù hợp; kiểm tra với TalkBack.

### 11.3. Ma trận kiểm tra thủ công

| Tình huống | Kết quả mong đợi |
| --- | --- |
| Tap nhanh, không di chuyển | Một click |
| Rung tay dưới slop | Chưa chuyển thành kéo |
| Kéo ra biên | Hình được giới hạn trong vùng nội dung |
| Parent intercept giữa gesture | Child nhận CANCEL, không click |
| Hai ngón pinch rồi nhấc ngón chính | Không nhảy hình, không click |
| Thêm ngón thứ ba | Không crash do index sai |
| View nhỏ hơn đường kính hình | Hình co theo vùng khả dụng |
| Drop đúng / sai vùng | Chỉ vùng hợp lệ cập nhật; highlight được dọn |
| Dọc / ngang / resize cửa sổ | Bố cục đúng, state hợp lý |
| API thấp nhất hỗ trợ / API có biến thể | Không gọi API ngoài phạm vi; resource fallback đúng |
| Dark mode / font lớn | Đủ tương phản, không cắt nhãn/nút |
| Disabled / TalkBack | Không nhận touch khi disabled; thao tác thay thế dùng được |

### 11.4. Rubric đánh giá đề xuất

| Hạng mục | Điểm |
| --- | ---: |
| ViewGroup, View và vòng đời | 15 |
| Custom View, đo kích thước, attributes | 20 |
| Canvas và quản lý đối tượng vẽ | 15 |
| Touch flow, drag, cancel | 15 |
| Multi Touch và Drag & Drop | 15 |
| Layout theo cấu hình và khôi phục state | 10 |
| Drawable XML/code và accessibility | 10 |
| **Tổng** | **100** |

Mức đạt đề xuất: từ 75 điểm; phải sửa các lỗi crash, pointer index sai và click khi CANCEL trước khi nghiệm thu.

## 12. Câu hỏi ôn tập và gợi ý đáp án

1. **ViewGroup có phải View không?** Có; nó bổ sung quản lý các child.
2. **Vì sao width trong constructor thường chưa dùng được?** Chưa hoàn tất layout.
3. **wrap_content có bảo đảm bằng kích thước nội dung?** Không; còn ràng buộc parent và logic đo của View.
4. **Đổi màu cần requestLayout không?** Thường chỉ cần invalidate nếu kích thước không đổi.
5. **Vì sao tránh tạo Paint trong onDraw?** Callback chạy nhiều lần; cấp phát lặp gây thêm chi phí.
6. **intercept true khác touch true thế nào?** Một cái chặn child, một cái báo đã xử lý event.
7. **Child nhận CANCEL khi nào?** Ví dụ parent lấy gesture hoặc hệ thống hủy chuỗi touch.
8. **Vì sao lưu pointer ID?** Index có thể đổi; ID theo pointer trong thời gian hoạt động.
9. **ACTION_POINTER_UP khác ACTION_UP?** Một pointer rời khi còn pointer khác, so với ngón cuối rời.
10. **Kéo hình bằng tọa độ có phải Android Drag & Drop?** Chưa; framework drag dùng ClipData/DragEvent.
11. **layout-v24 áp dụng cho app version 24?** Không; đó là Android API level tối thiểu của resource.
12. **Vì sao selector default đặt cuối?** Item khớp đầu tiên được chọn.
13. **Drawable có nhận touch riêng như View không?** Không; host View xử lý tương tác.
14. **mutate dùng để làm gì?** Tách state của Drawable khi cần thay đổi riêng instance.

## 13. Checklist tự đánh giá

- [ ] Tôi đọc được cây UI và chọn LayoutParams đúng parent.
- [ ] Tôi phân biệt attach/detach với measure/layout/draw.
- [ ] Tôi tạo Custom View dùng được bằng XML và code.
- [ ] Tôi xử lý MeasureSpec, wrap_content và padding.
- [ ] Tôi vẽ được hình/nhãn; dùng save/restore khi biến đổi Canvas.
- [ ] Tôi giải thích được dispatch/intercept/touch và ý nghĩa boolean.
- [ ] Tôi xử lý UP/CANCEL, slop và performClick đúng.
- [ ] Tôi phân biệt kéo bằng Touch với Drag & Drop của Android.
- [ ] Tôi theo dõi pointer ID và dùng ScaleGestureDetector.
- [ ] Tôi tạo resource theo orientation, width, API và dark mode.
- [ ] Tôi kiểm tra fallback và state khi configuration thay đổi.
- [ ] Tôi dùng shape, selector, ripple, vector và Drawable bằng code.

## 14. Hướng dẫn dùng nguồn tài liệu

Các link ở từng phần trỏ trực tiếp tới **Android Developers của Google**. Nội dung tiếng Việt, lộ trình, ví dụ và bài tập là phần biên soạn cho lớp intern; không phải bản dịch nguyên văn của một trang tài liệu.

Khi tra API, kiểm tra `Added in API level`, chữ ký phương thức và hành vi callback. Khi copy ví dụ vào dự án, kiểm tra namespace/package, minSdk, tài nguyên tham chiếu và parent thực tế. Đọc lại nguồn chính thức khi nâng SDK hoặc gặp hành vi khác với mô hình đơn giản trong bài.
