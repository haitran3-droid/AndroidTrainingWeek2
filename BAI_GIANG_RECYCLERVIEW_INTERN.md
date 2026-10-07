# Bài giảng Android: RecyclerView — Level Intern

**Đối tượng:** intern đã biết View/ViewGroup, Fragment và MVVM cơ bản.  
**Phạm vi:** Android Views, Kotlin, AndroidX RecyclerView và NestedScrollView.  
**Thời lượng gợi ý:** 4 buổi × 3 giờ, cộng thời gian thực hành.  
**Ngày đối chiếu nguồn:** 06/10/2026.  
**Bài trước:** [Mô hình MVVM](BAI_GIANG_MVVM_INTERN.md).

“Resue” trong yêu cầu được hiểu là **reuse/recycling: tái sử dụng View và ViewHolder**. “List trong List” gồm danh sách dọc chứa các dải cuộn ngang và cách xử lý các nhóm item cùng chiều cuộn.

Ví dụ có tên file được viết để đưa vào ứng dụng Android dùng Views. Thay package `com.example.recyclerlesson` bằng package thực tế; import `R` từ namespace của ứng dụng nếu cần. Các thư viện cần gồm RecyclerView, Core KTX, Fragment KTX, Lifecycle ViewModel/Runtime KTX, coroutines Android và AppCompat. Chọn phiên bản tương thích với cấu hình dự án; tra [AndroidX RecyclerView releases](https://developer.android.com/jetpack/androidx/releases/recyclerview) khi chọn hoặc nâng phiên bản.

**Trạng thái kiểm chứng:** đã kiểm tra cấu trúc Markdown, cú pháp XML và tham chiếu resource trong các ví dụ có tên file. Kotlin chưa được build/chạy vì workspace chưa có ứng dụng Android/Gradle. Phần 12 cung cấp ma trận kiểm tra khi triển khai.

## 1. Mục tiêu và lộ trình

| Nội dung | Sau bài học, intern làm được |
| --- | --- |
| Reuse | Giải thích create, bind, cache, pool và state của item |
| Nhiều ViewType | Thiết kế model, chọn layout/holder đúng và diff chính xác |
| List trong List | Dựng outer list dọc + inner list ngang; quản lý adapter và vị trí cuộn |
| NestedScrollView | Hiểu nested scrolling, đo kích thước và chọn cấu trúc cuộn phù hợp |
| MVVM | UI gửi action; ViewModel giữ state theo ID; Adapter render snapshot |

| Buổi | Nội dung | Thực hành |
| --- | --- | --- |
| 1 | RecyclerView, ViewHolder và reuse | Log create/bind/recycle; sửa checkbox bị sai khi cuộn |
| 2 | Multi ViewType, ListAdapter và DiffUtil | Feed có header, bài học và carousel |
| 3 | Nested RecyclerView | Shared pool, callback đúng item, giữ vị trí từng nhóm |
| 4 | NestedScrollView và kiểm chứng hiệu năng | So sánh list độc lập với list trong trang cuộn |

## 2. RecyclerView hoạt động với những thành phần nào?

RecyclerView hiển thị một cửa sổ vào tập dữ liệu, tạo và tái sử dụng View khi cần. Nó không tạo một View riêng cho mỗi item của toàn bộ dataset trong mọi cấu hình. [Nguồn: Create dynamic lists](https://developer.android.com/develop/ui/views/layout/recyclerview).

```text
ViewModel cung cấp danh sách immutable
             │ submitList
             ▼
Adapter: dữ liệu ↔ ViewHolder
             │
RecyclerView + LayoutManager
             │
Các item View đang hiển thị / được giữ để tái sử dụng
```

| Thành phần | Trách nhiệm |
| --- | --- |
| RecyclerView | Quản lý child View, cuộn, phối hợp recycling và cập nhật |
| Adapter | Tạo/bind holder; khai báo item count và ViewType |
| ViewHolder | Giữ itemView và tham chiếu các View bên trong |
| LayoutManager | Đo, bố trí item, quyết định hướng/list/grid |
| ItemDecoration | Vẽ/phân bổ khoảng cách hoặc divider |
| ItemAnimator | Animation khi item thêm, gỡ hoặc thay đổi |
| RecycledViewPool | Giữ holder đã recycle theo ViewType, có thể dùng chung |

LinearLayoutManager tạo list; GridLayoutManager tạo grid; StaggeredGridLayoutManager hỗ trợ grid có kích thước khác nhau theo item. Khi nhiều type trong grid, có thể đặt SpanSizeLookup để header chiếm toàn hàng. [Nguồn: Create dynamic lists](https://developer.android.com/develop/ui/views/layout/recyclerview).

## 3. Cơ chế reuse/recycling

### 3.1. Create khác bind

```text
Cần hiển thị item X, ViewType T
    │
    ├── Có holder phù hợp từ các nguồn tái sử dụng → dùng lại
    └── Chưa có → onCreateViewHolder(parent, T)
    │
    └── Cần gắn dữ liệu → onBindViewHolder(holder, position)
```

Create inflate layout và tạo holder; bind cập nhật nội dung. Một holder có thể lần lượt hiển thị nhiều item. Không đặt dữ liệu thay đổi theo item chỉ trong `onCreateViewHolder()`. [Nguồn: RecyclerView.Adapter API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter).

```text
Holder H ban đầu bind bài học ID 101
Cuộn xuống → H không còn cần hiển thị ở vị trí cũ
Sau đó H có thể bind bài học ID 209 cùng ViewType

H là ViewHolder cũ, dữ liệu của item đã đổi.
```

Không có quy tắc “holder ở vị trí 0 luôn dành cho item đầu tiên”. Một item có identity riêng; holder là phương tiện hiển thị có thể dùng lại.

### 3.2. Scrap, cache và pool

| Vùng/khái niệm | Cách hiểu cho bài học |
| --- | --- |
| Attached View | Child đang tham gia hiển thị/layout |
| Scrap | Holder tạm được giữ để dùng lại trong một lượt layout |
| Offscreen cache | Giữ một số View ngoài màn hình, còn biết thông tin dữ liệu đã bind |
| RecycledViewPool | Holder được phân loại theo ViewType để bind cho item thích hợp |
| Prefetch | Chuẩn bị một số item trước khi cần xuất hiện |

Đây là mô hình đơn giản, không phải một hàng đợi cố định “detach → cache → pool → bind” cho mọi item. Cache có thể cho phép dùng lại View mà không cần bind lại nếu dữ liệu vẫn hợp lệ. Tăng cache giữ nhiều View hơn và tăng bộ nhớ; không tự tăng tốc mọi màn hình. [Nguồn: RecyclerView API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView), [Mã nguồn RecyclerView](https://github.com/androidx/androidx/blob/androidx-main/recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/RecyclerView.java).

### 3.3. Callback cần nắm

| Callback | Mục đích |
| --- | --- |
| `onCreateViewHolder()` | Tạo layout/holder cho một ViewType |
| `onBindViewHolder()` | Gắn đầy đủ hoặc cập nhật một phần dữ liệu |
| `onViewAttachedToWindow()` | Item View được attach; không chứng minh toàn bộ item đang nhìn thấy |
| `onViewDetachedFromWindow()` | Item View detach; chưa chắc bị recycle ngay |
| `onViewRecycled()` | Cleanup tài nguyên/listener gắn với dữ liệu cũ khi recycle |

Không coi các callback của holder là lifecycle của Activity/Fragment. Không gọi API lấy dữ liệu mỗi lần bind vì cuộn hoặc diff có thể bind lại nhiều lần. [Nguồn: RecyclerView.Adapter API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter).

### 3.4. Full bind phải xử lý cả hai nhánh

```kotlin
// Sai: holder cũ từng có subtitle thì View vẫn có thể còn VISIBLE.
if (item.subtitle != null) {
    subtitle.text = item.subtitle
    subtitle.visibility = View.VISIBLE
}

// Đúng: mô tả toàn bộ trạng thái do dữ liệu quyết định.
subtitle.text = item.subtitle.orEmpty()
subtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
```

Cần xét text, ảnh/placeholder, visibility, checked/selected, enabled, alpha, progress, listener và accessibility content. Với ảnh tải bất đồng bộ, request cũ phải được thay/hủy theo cơ chế thư viện ảnh; không để kết quả item cũ gán vào holder đã bind item khác.

Với CheckBox/Switch/EditText, cập nhật từ code có thể gọi listener. Gỡ listener cũ → gán state → gắn listener mới. Checked state và dữ liệu nhập cần lưu theo **ID trong state/model**, không chỉ nằm trong View.

### 3.5. Position không phải identity

Không giữ `position` của lần bind trong click listener để truy cập dữ liệu về sau. Position có thể đổi khi insert/remove/diff; holder có thể trả `NO_POSITION`.

```kotlin
// Trong adapter có getItem() và holder:
holder.itemView.setOnClickListener {
    val positionNow = holder.bindingAdapterPosition
    if (positionNow == RecyclerView.NO_POSITION) return@setOnClickListener
    onItemClick(getItem(positionNow).id)
}
```

`bindingAdapterPosition` tương đối với adapter đã bind holder; `absoluteAdapterPosition` tương đối với adapter tổng của RecyclerView, hữu ích khi có ConcatAdapter. Nếu callback dùng snapshot item đã bind, gửi ID bất biến của snapshot và để tầng state kiểm tra item còn tồn tại. [Nguồn: ViewHolder API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder).

## 4. Nhiều ViewType

ViewType là số nguyên giúp RecyclerView biết holder/layout nào tương thích. Default type là 0; type không bắt buộc liên tiếp. Không dùng adapter position làm type: như vậy gần như mỗi item trở thành một loại và giảm khả năng reuse. [Nguồn: RecyclerView.Adapter API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter).

```text
FeedRow.Header    → TYPE_HEADER   → HeaderHolder   → item_feed_header.xml
FeedRow.Lesson    → TYPE_LESSON   → LessonHolder   → item_feed_lesson.xml
FeedRow.Carousel  → TYPE_CAROUSEL → CarouselHolder → item_feed_carousel.xml
```

Ba phần phải nhất quán: `getItemViewType()` → `onCreateViewHolder()` → `onBindViewHolder()`. Các dữ liệu dùng cùng layout/hành vi holder có thể dùng cùng type; khác title hay selected state thường không cần type mới.

### 4.1. Model rõ loại

Dùng sealed interface/class thay vì `Any` hoặc một model chứa quá nhiều trường nullable. Identity cho diff gồm loại row và ID ổn định. Nếu cùng ID nhưng chuyển sang loại khác, phải xử lý như item khác hoặc có quy tắc đổi type rõ ràng.

### 4.2. Một adapter hay ConcatAdapter?

| Nhu cầu | Cách phù hợp |
| --- | --- |
| Item nhiều loại xen kẽ | Một multi-type adapter |
| Header + nội dung + footer là các nhóm độc lập | ConcatAdapter ghép các adapter |
| Nhóm con có quy tắc dữ liệu riêng và cuộn ngang | Holder chứa RecyclerView con |

ConcatAdapter không tạo danh sách lồng nhau về View; nó ghép nhiều adapter thành dữ liệu cho **một RecyclerView**. Mặc định nó cô lập ViewType giữa các adapter con. Chỉ tắt isolation nếu cùng type thực sự dùng holder tương thích. [Nguồn: ConcatAdapter API](https://developer.android.com/reference/androidx/recyclerview/widget/ConcatAdapter), [ConcatAdapter.Config](https://developer.android.com/reference/androidx/recyclerview/widget/ConcatAdapter.Config).

## 5. ListAdapter và DiffUtil

ListAdapter dùng diff để tính thay đổi giữa list cũ/mới và gửi cập nhật tương ứng. Đây là lựa chọn gọn cho bài học; không cần tự gọi notifyDataSetChanged sau mỗi submitList. [Nguồn: ListAdapter API](https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter).

| Hàm | Câu hỏi cần trả lời |
| --- | --- |
| `areItemsTheSame(old, new)` | Có cùng item logic không? Thường dựa ID + loại |
| `areContentsTheSame(old, new)` | Những dữ liệu cần render có thay đổi không? |
| `getChangePayload(old, new)` | Có thể cập nhật riêng phần nào? Tùy chọn |

Không mutate list/item đang được diff hoặc đang được adapter dùng. Tạo snapshot mới bằng `map`, `copy` hoặc list mới. Stable IDs là cơ chế riêng của adapter; không cần bật stable IDs chỉ để dùng DiffUtil. Nếu bật, ID phải duy nhất và ổn định, không phải position. [Nguồn: DiffUtil API](https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil?authuser=1).

`submitList()` có thể xử lý diff bất đồng bộ. Commit callback của một list có thể không chạy nếu list đó bị thay thế bởi lần submit sau. Callback phù hợp cho thao tác UI của list đã commit; không dùng làm nơi bảo đảm thực hiện nghiệp vụ. [Nguồn: ListAdapter API](https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter).

Payload tối ưu update, không thay full bind. Payload có thể bị bỏ khi holder không attach; full bind vẫn phải render đúng mọi state. [Nguồn: RecyclerView.Adapter API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter).

## 6. List trong List: chọn cấu trúc trước

### 6.1. Dọc chứa ngang

```text
Outer RecyclerView: dọc
├── Header
├── Carousel A: RecyclerView ngang [A1][A2][A3]...
├── Lesson
├── Carousel B: RecyclerView ngang [B1][B2][B3]...
└── ...
```

Đây là cấu trúc phổ biến cho trang nội dung có nhiều dải thẻ. Outer và inner vẫn reuse độc lập. Giới hạn chiều cao inner để nó là một vùng cuộn ngang có kích thước rõ ràng. [Nguồn: Nested RecyclerViews](https://developer.android.com/topic/performance/issues/render).

### 6.2. Dọc chứa dọc

| Mục tiêu UI | Thiết kế thường phù hợp |
| --- | --- |
| Các nhóm đều cuộn chung theo chiều dọc | Flatten thành header + item trong một RecyclerView |
| Những nhóm độc lập nối tiếp | ConcatAdapter trong một RecyclerView |
| Vùng con thật sự cần cuộn dọc độc lập | RecyclerView con có chiều cao giới hạn, thiết kế gesture rõ |
| Trang ngắn, dữ liệu ít và có giới hạn | Có thể dùng NestedScrollView; đo hiệu năng thực tế |

“List trong List” không bắt buộc phải có hai RecyclerView cùng chiều. Nếu tất cả nội dung cần một lần cuộn dọc, thường một list lớn với nhiều type là cấu trúc đơn giản hơn.

### 6.3. Inner adapter và shared pool

- Tạo inner adapter/LayoutManager một lần trong constructor của outer holder, không tạo lại mỗi bind.
- Bind outer row thì submit dữ liệu của nhóm tương ứng cho inner adapter.
- Pool dùng chung giúp inner list reuse holder từ nhóm khác khi ViewType tương thích.
- Cùng số type trong pool phải có cùng holder/layout/hợp đồng bind; pool không tự hiểu class adapter.
- Không giữ callback của adapter tạo holder lần đầu: khi holder sang inner list khác, bind phải cập nhật listener cho dữ liệu/callback mới.

Shared pool không chia sẻ dataset hoặc vị trí cuộn; chỉ chia sẻ holder có thể reuse. Tăng pool/cache/prefetch phải có đo lường. [Nguồn: RecycledViewPool API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.RecycledViewPool?authuser=50), [Nested RecyclerViews](https://developer.android.com/topic/performance/issues/render).

### 6.4. Giữ vị trí theo section ID

Outer holder từng hiển thị carousel A có thể được bind carousel B. Nếu giữ nguyên LayoutManager state của holder, B có thể xuất hiện ở vị trí cuộn của A.

Giải pháp: lưu state theo ID của **nhóm dữ liệu**, không theo holder/position; khi bind nhóm mới, khôi phục state nhóm đó hoặc đưa về đầu. Với list thay đổi/reorder, nên giữ anchor bằng item ID + offset thay vì chỉ dựa index cũ.

Ví dụ phần 8 lưu Parcelable của LayoutManager theo section ID trong phạm vi adapter của một lần tạo UI. Muốn giữ qua xoay/process recreation, cần đưa state nhỏ phù hợp ra ngoài adapter và restore theo thiết kế; pool không làm việc này.

## 7. NestedScrollView và nested scrolling

### 7.1. NestedScrollView dùng cho việc gì?

NestedScrollView là container cuộn dọc, hỗ trợ vai trò nested scrolling parent và child. Nó không có cơ chế adapter/ViewHolder để recycle như RecyclerView. Nó chỉ nhận **một direct child**, thường là LinearLayout chứa nhiều View. [Nguồn: NestedScrollView API](https://developer.android.com/reference/androidx/core/widget/NestedScrollView.html), [Mã nguồn NestedScrollView](https://github.com/androidx/androidx/blob/androidx-main/core/core/src/main/java/androidx/core/widget/NestedScrollView.java).

```text
NestedScrollView
└── LinearLayout  ← một direct child
    ├── Tiêu đề
    ├── Nội dung/form
    ├── Danh sách ngắn nếu phù hợp
    └── Nút cuối trang
```

`fillViewport = true` làm content ngắn được kéo cao ít nhất tới vùng viewport theo cơ chế đo của container. Nó không tạo virtualization/recycling hoặc giới hạn item count của list bên trong.

### 7.2. Nested scrolling khác touch interception

Nested scrolling phối hợp **khoảng cuộn đã/đang tiêu thụ** giữa child và parent. Mô hình đơn giản:

```text
Child có delta cuộn
    → parent có cơ hội consume trước (pre-scroll)
    → child consume phần của nó
    → parent nhận phần đã consume và còn dư (post-scroll)
```

Touch interception quyết định View nào nhận chuỗi MotionEvent; nested scrolling phối hợp scroll distance/velocity. Chúng liên quan nhưng không phải cùng API. `requestDisallowInterceptTouchEvent(true)` không thay thế việc thiết kế nested scrolling. [Nguồn: NestedScrollingChild3](https://developer.android.com/reference/androidx/core/view/NestedScrollingChild3).

### 7.3. isNestedScrollingEnabled có nghĩa gì?

Đặt RecyclerView `isNestedScrollingEnabled = false` tắt việc nó tham gia giao thức nested scrolling với ancestor. **Không** có nghĩa RecyclerView không còn cuộn được hoặc không còn nhận touch; cũng không sửa được vấn đề đo kích thước.

Với list dọc ngắn muốn hòa vào trang NestedScrollView, có thể dùng height wrap_content và tắt nested scrolling trên list; toàn trang thường cuộn qua parent. Với vùng list dọc độc lập có height giới hạn, child vẫn có thể cuộn dù tắt nested scrolling, và phối hợp parent có thể kém hơn. Kiểm tra cả drag và fling.

### 7.4. Chiều cao không giới hạn có thể làm giảm lợi ích reuse

Khi RecyclerView dọc `wrap_content` nằm trong container cuộn dọc và được đo theo chiều cao không giới hạn phù hợp, nó có thể phải đo/layout rất nhiều hoặc toàn bộ item để tính chiều cao content. Viewport của RecyclerView lúc đó có thể gần bằng chiều cao toàn danh sách. Đây là suy luận từ cơ chế đo/layout, không phải khẳng định mọi tổ hợp LayoutManager/constraint luôn tạo toàn bộ item. [Nguồn: Mã nguồn NestedScrollView](https://github.com/androidx/androidx/blob/androidx-main/core/core/src/main/java/androidx/core/widget/NestedScrollView.java), [RecyclerView API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView).

Tắt nested scrolling không làm viewport nhỏ lại. Với dữ liệu dài, ưu tiên một RecyclerView có kích thước theo vùng màn hình, đưa header/footer vào adapter hoặc ConcatAdapter. Không cố sửa bằng setHasFixedSize, tăng cache hoặc tắt recycling.

## 8. Ví dụ chính: feed nhiều type và carousel ngang

### 8.1. Các file và phạm vi ví dụ

```text
java/com/example/recyclerlesson/
├── FeedModels.kt
├── FeedViewModel.kt
├── CourseAdapter.kt
├── FeedAdapter.kt
├── FeedFragment.kt
└── MainActivity.kt

res/layout/activity_main.xml
res/layout/fragment_feed.xml
res/layout/item_feed_header.xml
res/layout/item_feed_lesson.xml
res/layout/item_feed_carousel.xml
res/layout/item_course_card.xml
res/values/strings.xml
```

Feed mẫu có nhiều nhóm; dữ liệu nhỏ được tạo trong ViewModel để tập trung vào RecyclerView. Dự án thật lấy data từ repository như bài MVVM.

### 8.2. Model immutable, ID ổn định

File `FeedModels.kt`:

```kotlin
package com.example.recyclerlesson

data class CourseCard(val id: Long, val title: String)

sealed interface FeedRow {
    val key: String

    data class Header(override val key: String, val title: String) : FeedRow

    data class Lesson(
        val id: Long,
        val title: String,
        val subtitle: String? = null,
        val checked: Boolean = false
    ) : FeedRow {
        override val key: String get() = "lesson:$id"
    }

    data class Carousel(
        override val key: String,
        val title: String,
        val cards: List<CourseCard>
    ) : FeedRow
}
```

### 8.3. ViewModel giữ checkbox state theo ID

File `FeedViewModel.kt`:

```kotlin
package com.example.recyclerlesson

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FeedViewModel : ViewModel() {
    private val _rows = MutableStateFlow(createRows())
    val rows: StateFlow<List<FeedRow>> = _rows.asStateFlow()

    fun setChecked(id: Long, checked: Boolean) {
        _rows.update { current ->
            current.map { row ->
                if (row is FeedRow.Lesson && row.id == id) row.copy(checked = checked)
                else row
            }
        }
    }

    private fun createRows(): List<FeedRow> = buildList {
        repeat(30) { group ->
            add(FeedRow.Header("header:$group", "Nhóm bài học ${group + 1}"))
            add(
                FeedRow.Carousel(
                    key = "carousel:$group",
                    title = "Khóa học gợi ý",
                    cards = List(12) { index ->
                        CourseCard(group * 1_000L + index, "Khóa học ${index + 1}")
                    }
                )
            )
            repeat(6) { index ->
                add(
                    FeedRow.Lesson(
                        id = group * 100L + index,
                        title = "Bài học ${group + 1}.${index + 1}",
                        subtitle = if (index % 2 == 0) "Có bài thực hành" else null
                    )
                )
            }
        }
    }
}
```

30 × 8 = 240 outer rows, mỗi carousel có 12 cards. Checked state sống trong ViewModel; cuộn/recycle không làm mất. Ví dụ chưa lưu checked state bền vững qua process death.

### 8.4. Layout

File `res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/activityContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

File `res/layout/fragment_feed.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.recyclerview.widget.RecyclerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/feedRecycler"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="12dp"
    android:clipToPadding="false" />
```

File `res/layout/item_feed_header.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/headerTitle"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="12dp"
    android:textSize="20sp"
    android:textStyle="bold" />
```

File `res/layout/item_feed_lesson.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="12dp">
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        <TextView
            android:id="@+id/lessonTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
        <TextView
            android:id="@+id/lessonSubtitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </LinearLayout>
    <CheckBox
        android:id="@+id/lessonCheck"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
</LinearLayout>
```

File `res/layout/item_feed_carousel.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    <TextView
        android:id="@+id/carouselTitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp" />
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/courseRecycler"
        android:layout_width="match_parent"
        android:layout_height="120dp"
        android:paddingStart="12dp"
        android:paddingEnd="12dp"
        android:clipToPadding="false" />
</LinearLayout>
```

File `res/layout/item_course_card.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/courseTitle"
    android:layout_width="160dp"
    android:layout_height="match_parent"
    android:layout_marginEnd="8dp"
    android:padding="12dp"
    android:gravity="center"
    android:clickable="true"
    android:focusable="true"
    android:background="?android:attr/selectableItemBackground" />
```

File `res/values/strings.xml`; nếu đã có file, thêm item vào resources hiện tại:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">RecyclerView Lesson</string>
    <string name="mark_lesson">Đánh dấu bài học %1$s</string>
    <string name="course_clicked">Bạn chọn khóa học ID %1$d</string>
    <string name="short_page_title">Trang giới thiệu khóa học</string>
    <string name="short_page_footer">Tiếp tục</string>
</resources>
```

Layout `item_course_card` dành cho list ngang có chiều cao giới hạn. Không copy height match_parent vào một list dọc khác mà chưa xét ràng buộc đo.

### 8.5. Inner adapter: holder tương thích với shared pool

File `CourseAdapter.kt`:

```kotlin
package com.example.recyclerlesson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(
    private val onCourseClick: (Long) -> Unit
) : ListAdapter<CourseCard, CourseAdapter.CardHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = R.layout.item_course_card

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_card, parent, false)
        return CardHolder(view)
    }

    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        holder.bind(getItem(position), onCourseClick)
    }

    override fun onViewRecycled(holder: CardHolder) {
        holder.itemView.setOnClickListener(null)
        super.onViewRecycled(holder)
    }

    class CardHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.courseTitle)

        fun bind(item: CourseCard, onClick: (Long) -> Unit) {
            title.text = item.title
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onClick(item.id)
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CourseCard>() {
            override fun areItemsTheSame(old: CourseCard, new: CourseCard): Boolean =
                old.id == new.id

            override fun areContentsTheSame(old: CourseCard, new: CourseCard): Boolean =
                old == new
        }
    }
}
```

Listener được gán lại mỗi bind, không capture vĩnh viễn adapter/section đã tạo holder. Vì tất cả CourseAdapter có cùng type và CardHolder, chúng có thể dùng chung pool.

### 8.6. Outer adapter: nhiều type, bind đúng state, giữ scroll theo nhóm

File `FeedAdapter.kt`:

```kotlin
package com.example.recyclerlesson

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class FeedAdapter(
    private val sharedPool: RecyclerView.RecycledViewPool,
    private val onCheckedChanged: (Long, Boolean) -> Unit,
    private val onCourseClick: (Long) -> Unit
) : ListAdapter<FeedRow, RecyclerView.ViewHolder>(DIFF) {

    // Chỉ giữ vị trí trong phạm vi adapter này; key là identity của section.
    private val sectionScroll = mutableMapOf<String, Parcelable>()

    init {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is FeedRow.Header -> R.layout.item_feed_header
        is FeedRow.Lesson -> R.layout.item_feed_lesson
        is FeedRow.Carousel -> R.layout.item_feed_carousel
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.item_feed_header -> HeaderHolder(view)
            R.layout.item_feed_lesson -> LessonHolder(view)
            R.layout.item_feed_carousel -> CarouselHolder(view, sharedPool, onCourseClick)
            else -> error("Unsupported viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FeedRow.Header -> (holder as HeaderHolder).bind(row)
            is FeedRow.Lesson -> (holder as LessonHolder).bind(row, onCheckedChanged)
            is FeedRow.Carousel -> (holder as CarouselHolder).bind(row, sectionScroll)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is CarouselHolder) holder.saveScroll(sectionScroll)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is LessonHolder -> holder.clearListener()
            is CarouselHolder -> holder.recycle(sectionScroll)
        }
        super.onViewRecycled(holder)
    }

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.headerTitle)
        fun bind(row: FeedRow.Header) { title.text = row.title }
    }

    class LessonHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.lessonTitle)
        private val subtitle = view.findViewById<TextView>(R.id.lessonSubtitle)
        private val check = view.findViewById<CheckBox>(R.id.lessonCheck)

        fun bind(row: FeedRow.Lesson, onChange: (Long, Boolean) -> Unit) {
            title.text = row.title
            subtitle.text = row.subtitle.orEmpty()
            subtitle.visibility = if (row.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            check.setOnCheckedChangeListener(null)
            check.isChecked = row.checked
            check.contentDescription = itemView.context.getString(R.string.mark_lesson, row.title)
            check.setOnCheckedChangeListener { _, checked ->
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onChange(row.id, checked)
                }
            }
        }

        fun clearListener() { check.setOnCheckedChangeListener(null) }
    }

    class CarouselHolder(
        view: View,
        sharedPool: RecyclerView.RecycledViewPool,
        onCourseClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.carouselTitle)
        private val recycler = view.findViewById<RecyclerView>(R.id.courseRecycler)
        private val layout = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        private val cards = CourseAdapter(onCourseClick)
        private var sectionKey: String? = null
        private var bindVersion = 0L
        private var contentReady = false

        init {
            layout.initialPrefetchItemCount = 3 // Giá trị mẫu; cần đo theo số card nhìn thấy.
            recycler.layoutManager = layout
            recycler.adapter = cards
            recycler.setRecycledViewPool(sharedPool)
            recycler.setHasFixedSize(true)
        }

        fun saveScroll(states: MutableMap<String, Parcelable>) {
            if (!contentReady) return
            val key = sectionKey ?: return
            val state = layout.onSaveInstanceState() ?: return
            states[key] = state
        }

        fun bind(row: FeedRow.Carousel, states: MutableMap<String, Parcelable>) {
            saveScroll(states)
            recycler.stopScroll()
            sectionKey = row.key
            bindVersion += 1
            val version = bindVersion
            val targetState = states[row.key]
            contentReady = false
            title.text = row.title
            // Không cho người dùng tương tác với dữ liệu section cũ trong lúc diff.
            recycler.visibility = View.INVISIBLE
            cards.submitList(row.cards) {
                if (version == bindVersion && sectionKey == row.key) {
                    if (targetState != null) layout.onRestoreInstanceState(targetState)
                    else layout.scrollToPositionWithOffset(0, 0)
                    recycler.doOnNextLayout {
                        if (version == bindVersion && sectionKey == row.key) {
                            contentReady = true
                            recycler.visibility = View.VISIBLE
                        }
                    }
                    recycler.requestLayout()
                }
            }
        }

        fun recycle(states: MutableMap<String, Parcelable>) {
            saveScroll(states)
            recycler.stopScroll()
            bindVersion += 1 // Callback của bind cũ không được restore vào section mới.
            sectionKey = null
            contentReady = false
            recycler.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FeedRow>() {
            override fun areItemsTheSame(old: FeedRow, new: FeedRow): Boolean =
                old::class == new::class && old.key == new.key

            override fun areContentsTheSame(old: FeedRow, new: FeedRow): Boolean =
                old == new
        }
    }
}
```

Inner list có chiều cao 120dp và width theo parent, nên kích thước RecyclerView đó không phụ thuộc số card; đây là lý do dùng `setHasFixedSize(true)` ở inner. Nó không có nghĩa mọi card hoặc mọi outer row bằng nhau.

Commit callback chỉ khôi phục UI của list đã commit. Token `bindVersion` chặn callback cũ cập nhật holder đã đổi section; `contentReady` chỉ được bật sau lượt layout áp dụng dữ liệu/vị trí mới, tránh lưu nhầm vị trí cũ vào section mới. `sectionScroll` của bài mẫu phù hợp dữ liệu card ổn định trong mỗi nhóm; nếu card bị reorder/xóa, cần chính sách anchor theo card ID. Với section động, cần dọn state của key đã bị xóa để map không tăng mãi.

`PREVENT_WHEN_EMPTY` giúp trì hoãn restore của RecyclerView khi adapter đang rỗng chờ dữ liệu. Nó không bảo đảm restore đúng nếu dataset sau đó hoàn toàn khác hoặc muốn anchor theo ID logic. [Nguồn: Mã nguồn RecyclerView](https://github.com/androidx/androidx/blob/androidx-main/recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/RecyclerView.java).

### 8.7. Fragment và Activity

File `FeedFragment.kt`:

```kotlin
package com.example.recyclerlesson

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FeedFragment : Fragment(R.layout.fragment_feed) {
    private val viewModel: FeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.feedRecycler)
        val pool = RecyclerView.RecycledViewPool()
        val adapter = FeedAdapter(
            sharedPool = pool,
            onCheckedChanged = viewModel::setChecked,
            onCourseClick = { id ->
                Toast.makeText(
                    requireContext(), getString(R.string.course_clicked, id), Toast.LENGTH_SHORT
                ).show()
            }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows -> adapter.submitList(rows) }
            }
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.feedRecycler)?.adapter = null
        super.onDestroyView()
    }
}
```

Pool/adapter thuộc một lần tạo View; không lưu holder, pool hoặc adapter vào ViewModel vì chúng có thể giữ View/Context/callback UI. Khi cần restore inner scroll qua View recreation, đưa dữ liệu scroll phù hợp ra state holder, không đưa View vào đó.

File `MainActivity.kt`:

```kotlin
package com.example.recyclerlesson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.activityContainer, FeedFragment::class.java, null)
                .commit()
        }
    }
}
```

Manifest cần khai báo MainActivity và theme AppCompat. Ví dụ không cần Internet permission. Toast chỉ minh họa click tại thời điểm người dùng thao tác; ứng dụng thật có thể điều hướng bằng course ID.

### 8.8. Thực hành với ví dụ

1. Check bài đầu, cuộn xa rồi quay lại: đúng bài vẫn checked, bài khác không bị checked nhầm.
2. Cuộn carousel A sang card xa; cuộn outer để holder được dùng lại cho B: B không lấy vị trí của A.
3. Quay lại A: vị trí của A được giữ trong adapter hiện tại.
4. Click card sau nhiều lượt recycle: callback có ID của card đang hiển thị.
5. Xoay: checked state còn theo ViewModel; inner scroll chưa được giữ qua View recreation trong bài mẫu.
6. Bỏ listener reset hoặc bỏ nhánh subtitle GONE để tái hiện lỗi rồi sửa.

## 9. Ví dụ NestedScrollView cho trang ngắn

Đây là ví dụ riêng, không bọc `fragment_feed.xml` có 240 outer rows bằng NestedScrollView.

File `res/layout/fragment_short_page.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/pageScroll"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/short_page_title"
            android:textSize="24sp" />
        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/shortRecycler"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
        <Button
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/short_page_footer" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

Trong `onViewCreated()` của Fragment dùng layout này, import RecyclerView/LinearLayoutManager như ví dụ chính:

```kotlin
val shortRecycler = view.findViewById<RecyclerView>(R.id.shortRecycler)
shortRecycler.layoutManager = LinearLayoutManager(requireContext())
shortRecycler.isNestedScrollingEnabled = false

val shortAdapter = FeedAdapter(
    sharedPool = RecyclerView.RecycledViewPool(),
    onCheckedChanged = { _, _ -> /* Bài nâng cấp: gọi ViewModel để cập nhật state. */ },
    onCourseClick = { _ -> }
)
shortRecycler.adapter = shortAdapter
shortAdapter.submitList(
    listOf(
        FeedRow.Header("short:header", "Nội dung khóa học"),
        FeedRow.Lesson(1, "RecyclerView cơ bản"),
        FeedRow.Lesson(2, "Nhiều ViewType")
    )
)
```

Ví dụ chỉ minh họa layout/scroll của danh sách 3 rows, chưa cập nhật checkbox state. Khi dùng thật, nối callback vào ViewModel như phần 8, hoặc chuyển row sang loại chỉ đọc. Không đặt `setHasFixedSize(true)` chỉ vì số item ít: chiều cao list wrap_content này phụ thuộc nội dung.

**Bài so sánh:** tăng số rows lên 1.000 và log số create/bind, thời gian layout, bộ nhớ; so với RecyclerView độc lập có height match_parent. Kết luận theo đo lường, không chỉ theo cảm giác “cuộn vẫn được”.

## 10. Hiệu năng và các hiểu nhầm cần tránh

### 10.1. setHasFixedSize đúng nghĩa

`setHasFixedSize(true)` báo kích thước **RecyclerView** không bị ảnh hưởng bởi thay đổi nội dung adapter theo hợp đồng API, giúp tối ưu tính toán layout. Không có nghĩa các item bằng kích thước, không khóa chiều cao, không tắt diff và không chữa được list bị đo quá rộng/cao. [Nguồn: RecyclerView API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView).

### 10.2. Cache, pool và prefetch

| Cấu hình | Tác động | Cần kiểm tra |
| --- | --- | --- |
| setItemViewCacheSize | Giữ nhiều View offscreen của RecyclerView | Đổi bộ nhớ lấy giảm bind/create trong tình huống cụ thể |
| setRecycledViewPool | Chia sẻ holder giữa các RecyclerView | ViewType và holder phải tương thích |
| setMaxRecycledViews(type, count) | Giới hạn pool cho một type | Không đặt quá lớn khi chưa đo |
| initialPrefetchItemCount | Chuẩn bị inner items khi nested | Chọn gần số card cần xuất hiện, kiểm tra chi phí |

Prefetch không tạo trước toàn bộ danh sách. Inner list nhìn thấy khoảng 2–3 cards không tự cần prefetch 100. [Nguồn: LinearLayoutManager API](https://developer.android.com/reference/androidx/recyclerview/widget/LinearLayoutManager), [RecycledViewPool API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.RecycledViewPool?authuser=50).

### 10.3. Điều cần giữ nhẹ ở bind

- Tránh network, đọc file, decode ảnh lớn hoặc tính toán nặng mỗi lần bind.
- Inflate ít lớp View hợp lý; đo layout khi nghi ngờ hierarchy nặng.
- Dùng diff/update đúng phần thay vì refresh toàn danh sách liên tục.
- Không tạo inner adapter/LayoutManager mới mỗi bind.
- Không dùng `setIsRecyclable(false)` cho mọi holder để che lỗi state.
- Tăng cache/pool và tắt animation chỉ khi nguyên nhân đã được kiểm chứng.

Nếu một holder tạm không thể recycle vì animation/tài nguyên cụ thể, phải quản lý thời điểm và cặp lời gọi theo API. Detach có thể tạm thời; cleanup trong onViewRecycled không thay thế full bind chính xác. [Nguồn: ViewHolder API](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder).

## 11. Debug create, bind và recycle

Thêm log tạm trong adapter, dùng `System.identityHashCode(holder)` để nhận biết holder instance:

```kotlin
// Trong onBindViewHolder():
Log.d("RecyclerLesson", "bind holder=${System.identityHashCode(holder)} position=$position")

// Trong onViewRecycled():
Log.d("RecyclerLesson", "recycle holder=${System.identityHashCode(holder)} type=${holder.itemViewType}")
```

Log create sau khi đã tạo holder trong onCreateViewHolder; ghi thêm row key/card ID ở bind. Scroll qua nhiều item và xem cùng holder bind các ID khác nhau. Số holder không bắt buộc bằng số item nhìn thấy vì cache, prefetch, animation và pool.

| Triệu chứng | Điều cần kiểm tra |
| --- | --- |
| Checkbox nhảy sang item khác | State lưu theo ID chưa? Bind có reset listener/state không? |
| Subtitle/ảnh của item cũ còn xuất hiện | Full bind có gán cả nhánh có/không có dữ liệu không? |
| Click sai item hoặc index lỗi | Capture position cũ? Có xử lý NO_POSITION không? |
| ClassCastException khi dùng pool | Cùng viewType có holder/layout khác nhau không? |
| Carousel B nhận scroll của A | State có theo section ID hay theo holder? |
| Inner list nhấp nháy/reset khi outer cập nhật | Tạo adapter lại mỗi bind? Restore có đúng timing/key không? |
| submitList không cập nhật | Có mutate/re-submit cùng list instance? Diff so sánh đúng không? |
| Trang dài tạo nhiều View ngay từ đầu | RecyclerView có bị đo cao theo toàn content không? |
| Cuộn giật ở lần gặp carousel mới | Có shared pool phù hợp? Layout/ảnh/bind nặng ở đâu? |
| UI/View còn được giữ sau onDestroyView | Adapter/pool/callback có được lưu quá scope UI không? |

Đo bằng công cụ profiling/rendering phù hợp và thiết bị đại diện. Log hỗ trợ hiểu hành vi; lượng log lớn tự làm ảnh hưởng tốc độ, nên tắt khi đo hiệu năng chính thức. [Nguồn: Slow rendering](https://developer.android.com/topic/performance/issues/render).

## 12. Bài tập, kiểm tra và đánh giá

### 12.1. Bài tập từng phần

| Bài | Yêu cầu | Tiêu chí đạt |
| --- | --- | --- |
| A — Reuse | List 1.000 rows, log create/bind/recycle | Giải thích một holder bind nhiều item và cache có thể tránh bind |
| B — State | Checkbox, subtitle tùy chọn và ảnh | Cuộn xa không làm nhầm state/dữ liệu |
| C — Multi-type | Header, item, loading/error/footer | Mapping model/type/layout/holder nhất quán |
| D — Diff | Insert/remove/reorder/update checked | ID đúng; không mutate list; callback đúng item |
| E — Nested | Feed dọc chứa carousel ngang | Reuse adapter, shared pool đúng và giữ scroll theo nhóm |
| F — NestedScrollView | Trang ngắn; thử list dài để so sánh | Giải thích nested scrolling và vấn đề đo viewport |

### 12.2. Bài tổng hợp: Learning Feed

Xây feed có header, banner hoặc carousel, bài học có checkbox, footer loading/error. Nguồn dữ liệu nằm trong repository/ViewModel; UI collect state theo lifecycle và submit immutable list.

Yêu cầu nghiệm thu:

- Ít nhất ba ViewType, full bind đúng mọi state và diff theo identity.
- Checkbox được giữ theo lesson ID; insert/reorder không làm click cập nhật sai bài.
- Carousel dùng inner adapter/LayoutManager ổn định và shared pool tương thích.
- Vị trí cuộn của từng carousel gắn section ID; không lấy scroll từ holder cũ.
- Thêm cơ chế restore inner scroll qua View recreation, có giới hạn state lưu.
- Khi đổi data của carousel, anchor không nhảy sang card không liên quan.
- Chọn một RecyclerView/ConcatAdapter cho list dọc dài; giải thích lựa chọn NestedScrollView nếu dùng.
- UI render/callback không gọi API trong bind; không giữ View/adapter trong ViewModel.

### 12.3. Ma trận kiểm tra thủ công

| Tình huống | Kết quả mong đợi |
| --- | --- |
| Cuộn qua 1.000 outer items | Không tạo một holder mới cho mọi item chỉ vì item xuất hiện |
| Check một bài, cuộn xa/quay lại | Cùng ID giữ state, ID khác không bị ảnh hưởng |
| Item có subtitle → item không subtitle | Subtitle bị ẩn và text cũ không còn |
| Chèn/xóa/reorder lúc đang thao tác | Callback gửi đúng ID; không crash NO_POSITION |
| Đổi loại row cùng ID | Diff/type policy đúng, không cast nhầm holder |
| Carousel A được thay bằng B trên cùng holder | B restore theo state B hoặc về đầu |
| Quay lại carousel A | Restore theo section ID |
| Submit inner list nhanh nhiều lần | Callback cũ không cập nhật holder/section mới |
| Shared pool giữa nhiều inner list | Holder đúng type và callback đúng dữ liệu mới |
| Drag ngang, drag dọc và đường chéo | Gesture phù hợp; parent/child không chặn sai vô điều kiện |
| Fling outer/inner tới biên | Không bị kẹt cuộn, kiểm tra nested scroll policy |
| Xoay màn hình | Restore theo phạm vi đã thiết kế; không giữ View cũ |
| NestedScrollView với 3 và 1.000 rows | So sánh create/bind/layout/bộ nhớ và chọn cấu trúc phù hợp |
| Font lớn, TalkBack, RTL | Nội dung, nhãn checkbox và hướng layout dùng được |

Không đặt tiêu chí cứng như “1000 item chỉ được tạo 10 holder”: số lượng phụ thuộc type, viewport, cache, prefetch và cấu hình layout. Đánh giá dựa trên hành vi và kết quả đo.

### 12.4. Rubric đánh giá

| Hạng mục | Điểm |
| --- | ---: |
| Giải thích reuse/cache/pool và full bind | 25 |
| Multi ViewType và DiffUtil | 25 |
| List trong List, shared pool và state cuộn | 25 |
| NestedScrollView và chọn cấu trúc cuộn | 15 |
| MVVM, lifecycle và kiểm chứng | 10 |
| **Tổng** | **100** |

Mức đạt đề xuất: từ 75 điểm. Cần sửa lỗi state sai khi recycle, callback sai ID và holder không tương thích pool trước khi nghiệm thu.

## 13. Câu hỏi ôn tập và đáp án ngắn

1. **RecyclerView có tạo View cho toàn bộ dataset không?** Không phải luôn; viewport/ràng buộc đo và LayoutManager quyết định số View cần.
2. **Create và bind khác nhau thế nào?** Create tạo holder/layout; bind gắn dữ liệu cho item.
3. **Một holder có thể đại diện nhiều item không?** Có, lần lượt qua các lần reuse.
4. **Detach có đồng nghĩa recycle ngay không?** Không; View có thể còn trong cache hoặc được attach lại.
5. **Mỗi lần dùng lại View có chắc gọi bind không?** Không; cache hợp lệ có thể dùng lại mà không bind.
6. **Vì sao subtitle cũ còn hiện?** Bind thiếu nhánh reset khi item mới không có subtitle.
7. **Checked state nên ở đâu?** Model/UI state theo ID, View chỉ render và gửi action.
8. **Có nên dùng position làm ID/ViewType?** Không; position thay đổi và không phải identity/layout contract.
9. **bindingAdapterPosition có thể NO_POSITION không?** Có; phải kiểm tra trước khi truy cập dữ liệu.
10. **areItemsTheSame khác areContentsTheSame?** Một cái so identity, một cái so dữ liệu cần render.
11. **Payload thay full bind được không?** Không; full bind vẫn phải đúng vì payload có thể không được giao.
12. **ConcatAdapter có tạo RecyclerView lồng nhau?** Không; nó ghép adapter cho một RecyclerView.
13. **Shared pool có giữ data/scroll của section không?** Không; chỉ chia sẻ holder theo type.
14. **Cùng type số 0 ở hai adapter có chắc reuse chung được?** Không; holder/layout/hành vi phải tương thích.
15. **Vì sao inner list không nên tạo adapter mỗi bind?** Làm mất state và tăng cấp phát/thiết lập lại không cần thiết.
16. **NestedScrollView có recycle View như RecyclerView không?** Không.
17. **NestedScrollView có bao nhiêu direct child?** Một; bọc nhiều View bằng một container.
18. **nestedScrollingEnabled=false có tắt cuộn của RecyclerView không?** Không; tắt giao thức phối hợp với ancestor.
19. **fillViewport có làm list lớn recycle tốt hơn không?** Không; nó xử lý content ngắn theo viewport.
20. **setHasFixedSize(true) có nghĩa item cao bằng nhau?** Không; nó nói về kích thước RecyclerView và sự phụ thuộc vào nội dung.

## 14. Checklist tự đánh giá

- [ ] Tôi giải thích được create/bind/recycle và holder không phải identity item.
- [ ] Tôi phân biệt attached, scrap, cache, pool và prefetch ở mức cơ bản.
- [ ] Tôi full bind cả state có/không có dữ liệu và quản lý listener đúng.
- [ ] Tôi lưu checked/input state theo ID, không chỉ ở View.
- [ ] Tôi xử lý position hiện tại và NO_POSITION đúng.
- [ ] Tôi thiết kế nhiều ViewType với model/layout/holder nhất quán.
- [ ] Tôi dùng ListAdapter/DiffUtil với list/item immutable.
- [ ] Tôi biết khi nào chọn multi-type adapter, ConcatAdapter hoặc nested RecyclerView.
- [ ] Tôi tạo inner adapter/LayoutManager một lần và pool tương thích.
- [ ] Tôi giữ vị trí cuộn theo section ID, xét restore khi data thay đổi.
- [ ] Tôi hiểu NestedScrollView, một direct child và fillViewport.
- [ ] Tôi phân biệt nested scrolling với touch interception.
- [ ] Tôi biết tắt nested scrolling không sửa vấn đề đo kích thước.
- [ ] Tôi chọn cấu trúc cuộn phù hợp cho list dài và kiểm chứng bằng đo lường.

## 15. Hướng dẫn đọc nguồn

Nguồn chính là **Android Developers của Google**. Cơ chế đo/reuse và các chi tiết triển khai được đối chiếu thêm với **mã nguồn AndroidX chính thức**; branch androidx-main có thể khác phiên bản dependency của dự án, nên luôn kiểm tra API/version đang dùng khi debug. Link nguồn đặt tại phần liên quan.

Lộ trình, giải thích tiếng Việt, ví dụ và bài tập được biên soạn cho intern. Khi debug, lần theo **ID dữ liệu → ViewType → holder đang bind → state đã reset → callback → viewport và cơ chế cuộn**; không chữa lỗi reuse bằng cách tắt reuse toàn bộ.
