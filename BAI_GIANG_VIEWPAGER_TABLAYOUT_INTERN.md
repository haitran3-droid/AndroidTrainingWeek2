# Bài giảng Android: ViewPager, ViewPager2 và TabLayout — Level Intern

**Đối tượng:** intern đã biết View/ViewGroup, Fragment, MVVM và RecyclerView.  
**Phạm vi:** Android Views, Kotlin, AndroidX và Material Components.  
**Thời lượng gợi ý:** 4 buổi × 3 giờ, cộng thời gian thực hành.  
**Ngày đối chiếu nguồn:** 06/10/2026.  
**Bài trước:** [RecyclerView](BAI_GIANG_RECYCLERVIEW_INTERN.md).  
**Kiến thức liên quan:** [Fragment/DialogFragment](BAI_GIANG_FRAGMENT_DIALOGFRAGMENT_INTERN.md), [MVVM](BAI_GIANG_MVVM_INTERN.md).

“Listener Page Change” là callback theo dõi trang được chọn, tiến trình cuộn và trạng thái cuộn. Bài học dùng ViewPager2 làm ví dụ chính; ViewPager và adapter Fragment cũ được học để đọc, bảo trì và chuyển đổi ứng dụng dùng Views.

Ví dụ có tên file được thiết kế để đưa vào một ứng dụng Android. Thay package `com.example.pagerlesson` bằng package thực tế; import `R` từ namespace của ứng dụng nếu cần. Workspace hiện chỉ có tài liệu: Kotlin chưa được build/chạy. Markdown, XML và tham chiếu resource trong tài liệu được kiểm tra tĩnh; ma trận kiểm thử nằm ở phần 13.

## 1. Mục tiêu và lộ trình

| Nội dung | Sau bài học, intern làm được |
| --- | --- |
| Use case | Chọn pager cho các trang ngang cấp; giải thích khi nào nên dùng list hoặc Navigation |
| Adapter | Phân biệt adapter View/Fragment, adapter cũ/mới và cách giữ state |
| TabLayout | Kết nối hai chiều giữa tab và trang với đúng API |
| Custom UI | Đổi indicator, màu, icon, badge và layout riêng của tab |
| Change page | Điều hướng bằng vuốt, tab, nút và `setCurrentItem()` |
| Listener | Đọc ba callback, tránh vòng lặp và gỡ callback đúng lifecycle |
| Lifecycle/state | Dùng đúng FragmentManager, xử lý xoay máy và danh sách trang thay đổi |

| Buổi | Nội dung | Bài thực hành |
| --- | --- | --- |
| 1 | Use case, so sánh pager, adapter và lifecycle | Chọn adapter cho gallery, onboarding, màn hình ba tab |
| 2 | ViewPager2 + FragmentStateAdapter + TabLayoutMediator | Dựng màn hình ba trang, nút Trước/Sau |
| 3 | Custom tab và page callback | Thêm badge, selected state, log hành vi đổi trang |
| 4 | ViewPager cũ, state và trang động | Chuyển ví dụ cũ sang ViewPager2; kiểm tra xoay máy/xóa trang |

## 2. Use case: pager giải quyết vấn đề gì?

Pager cho phép chuyển giữa các trang nội dung. TabLayout thể hiện những lựa chọn tương ứng; nó không tự tạo nội dung trang.

```text
Người dùng vuốt / bấm tab / bấm nút
                 │
                 ▼
             Pager đổi trang
                 │
      ┌──────────┴───────────┐
      ▼                      ▼
Tab được chọn          Callback cập nhật UI
```

| Tình huống | Cách dùng phù hợp | Điểm cần cân nhắc |
| --- | --- | --- |
| Tổng quan / Bài tập / Kết quả | Pager + tab có tên rõ ràng | Ba màn hình cùng cấp trong một tính năng |
| Gallery ảnh | Pager với View-based adapter | Tải ảnh theo nhu cầu; xử lý gesture zoom |
| Onboarding vài bước | Pager + indicator + Trước/Sau | Không bắt buộc dùng TabLayout nếu tab không có ý nghĩa |
| Danh mục tin tức | Pager + tab cuộn ngang | Cần ID ổn định nếu server thay đổi danh mục |
| Feed hàng nghìn item | RecyclerView cho danh sách | Item trong feed thường không phải một trang toàn màn hình |
| Đi từ danh sách đến chi tiết | Navigation/Fragment transaction | Đây là quan hệ cha–con và có back stack |
| Form phải xác nhận từng bước | Điều hướng có kiểm soát | Tab hoặc vuốt có thể bỏ qua validation nếu không thiết kế luồng |

TabLayout độc lập cũng có thể phát sự kiện đổi nội dung do ứng dụng tự quản lý. Khi ghép với pager, dùng cơ chế kết nối sẵn để giữ tab và trang đồng bộ. [Nguồn: Swipe views với ViewPager2](https://developer.android.com/guide/navigation/advanced/swipe-view-2).

### 2.1 Chọn ViewPager hay ViewPager2?

| Đặc điểm | ViewPager | ViewPager2 |
| --- | --- | --- |
| Package | `androidx.viewpager.widget` | `androidx.viewpager2.widget` |
| Adapter View | `PagerAdapter` | `RecyclerView.Adapter` / `ListAdapter` |
| Adapter Fragment | `FragmentPagerAdapter`, `FragmentStatePagerAdapter` | `FragmentStateAdapter` |
| Hướng phân trang | Ngang | Ngang hoặc dọc |
| RTL | Khả năng hạn chế hơn | Có hỗ trợ RTL |
| Kết nối TabLayout | `setupWithViewPager()` | `TabLayoutMediator` |
| Theo dõi trang | `OnPageChangeListener` | `OnPageChangeCallback` |
| Trọng tâm học | Bảo trì code cũ, hiểu migration | Thực hành pager trong chương trình Android Views |

ViewPager2 dựa trên RecyclerView nên kiến thức tạo/bind/reuse ViewHolder từ bài trước vẫn áp dụng cho adapter View. [Nguồn: Migration guide](https://developer.android.com/develop/ui/views/animations/vp2-migration).

**Tình trạng thư viện:** AndroidX hiện ghi ViewPager2 ở chế độ bảo trì, tập trung sửa lỗi quan trọng và không lên kế hoạch tính năng mới; hướng phát triển UI mới được đề cập là Compose. Bài này vẫn dạy ViewPager2 vì chương trình đang học Android Views và cần làm việc với ứng dụng hiện có. [Nguồn: ViewPager2 releases](https://developer.android.com/jetpack/androidx/releases/viewpager2).

## 3. Các loại adapter: không dùng lẫn tên và trách nhiệm

| Adapter | Dùng với | Nội dung trang | Cách quản lý trang |
| --- | --- | --- | --- |
| `PagerAdapter` | ViewPager | View hoặc đối tượng do ứng dụng quản lý | Tự tạo/gắn/gỡ View; trả về key nhận diện trang |
| `FragmentPagerAdapter` | ViewPager | Fragment | Giữ Fragment instance; View của Fragment ngoài vùng cần thiết có thể bị hủy |
| `FragmentStatePagerAdapter` | ViewPager | Fragment | Có thể hủy Fragment ngoài vùng cần thiết và lưu state để tạo lại |
| `RecyclerView.Adapter<VH>` | ViewPager2 | View | Tạo/bind/reuse ViewHolder |
| `ListAdapter<T, VH>` | ViewPager2 | View | Là RecyclerView adapter có hỗ trợ diff danh sách |
| `FragmentStateAdapter` | ViewPager2 | Fragment | Quản lý tạo lại Fragment và saved state theo item ID |

`FragmentStateAdapter` kế thừa RecyclerView.Adapter, không kế thừa `PagerAdapter` cũ. Hai adapter Fragment của ViewPager đã deprecated; dùng chúng để hiểu code cũ, ưu tiên hướng chuyển đổi trong phạm vi Views. [Nguồn: FragmentPagerAdapter API](https://developer.android.com/reference/androidx/fragment/app/FragmentPagerAdapter), [mã nguồn FragmentStatePagerAdapter](https://github.com/androidx/androidx/blob/androidx-main/fragment/fragment/src/main/java/androidx/fragment/app/FragmentStatePagerAdapter.java), [FragmentStateAdapter API](https://developer.android.com/reference/androidx/viewpager2/adapter/FragmentStateAdapter).

### 3.1 Những hàm intern cần giải thích được

**PagerAdapter:**

- `getCount()`: số trang.
- `instantiateItem(container, position)`: tạo trang, gắn vào container, trả về key.
- `destroyItem(container, position, object)`: gỡ trang tương ứng key.
- `isViewFromObject(view, object)`: xác định View thuộc key nào; nếu trả root View làm key thì dùng `view === object`.
- `getPageTitle(position)`: tiêu đề cho TabLayout khi dùng `setupWithViewPager()`.

**RecyclerView.Adapter:** `getItemCount()`, `onCreateViewHolder()`, `onBindViewHolder()` và `getItemViewType()` nếu nhiều kiểu trang. Bind đầy đủ state của View; không để dữ liệu cũ sót lại khi reuse.

**FragmentStateAdapter:** `getItemCount()`, `createFragment(position)`; thêm `getItemId()` và `containsItem()` khi danh sách có thêm/xóa/đổi thứ tự. Mỗi lần `createFragment()` được gọi phải trả một Fragment mới phù hợp item; không tạo sẵn rồi cache toàn bộ Fragment trong một list.

### 3.2 Chọn host và FragmentManager đúng

| Pager nằm ở đâu? | Constructor thông dụng | Phạm vi quản lý |
| --- | --- | --- |
| Trong Fragment | `FragmentStateAdapter(this)` trong Fragment host | Fragment con của host |
| Trong FragmentActivity/AppCompatActivity | `FragmentStateAdapter(this)` trong Activity | Fragment thuộc Activity |
| Cần chỉ rõ manager/lifecycle | `FragmentStateAdapter(manager, lifecycle)` | Phải chọn cả hai tương ứng với host |

Ví dụ pager bên trong `CourseFragment`: trang `OverviewFragment` và `PracticeFragment` là con của `CourseFragment`. Dùng constructor nhận Fragment để adapter chọn `childFragmentManager` và lifecycle của Fragment host. Không thay bằng `requireActivity().supportFragmentManager` chỉ để hết lỗi.

Khi sửa code ViewPager cũ bên trong Fragment, truyền `childFragmentManager` cho adapter Fragment. Ôn lại sự khác nhau giữa manager quản lý **chính host** và manager quản lý **con của host** ở bài Fragment.

### 3.3 Lifecycle của một trang Fragment

Không có quy tắc “mỗi lần vuốt gọi lại createFragment” hay “mọi trang đã tạo đều đang RESUMED”. Adapter có thể chuẩn bị trang lân cận trước khi người dùng nhìn thấy nó.

Với FragmentStateAdapter, trang hiện tại được phép lên RESUMED khi lifecycle host cho phép; các trang khác được giữ thường bị giới hạn ở STARTED. Khi host bị dừng, trang cũng không thể duy trì RESUMED chỉ vì đang được chọn. Việc giới hạn lifecycle được phối hợp với trạng thái cuộn. [Nguồn: mã nguồn FragmentStateAdapter](https://github.com/androidx/androidx/blob/androidx-main/viewpager2/viewpager2/src/main/java/androidx/viewpager2/adapter/FragmentStateAdapter.java).

Hệ quả thực hành:

- `onViewCreated()` dùng để thiết lập View, không chứng minh trang vừa được người dùng chọn.
- Collect Flow bằng `viewLifecycleOwner`; chọn STARTED hay RESUMED theo nhu cầu. STARTED có thể vẫn chạy ở trang lân cận.
- Nếu video chỉ được phát trên trang đang chọn, thiết kế điều kiện selected + lifecycle phù hợp; không chỉ nhìn việc Fragment đã được tạo.
- Không giữ ViewBinding sau `onDestroyView()`.
- State nghiệp vụ nên ở ViewModel/repository theo ID; View state nhỏ có thể dùng saved state.

## 4. Kết nối pager với TabLayout

### 4.1 ViewPager: setupWithViewPager

```kotlin
pager.adapter = adapter
tabs.setupWithViewPager(pager)
```

TabLayout lấy tiêu đề từ `adapter.getPageTitle(position)` và kết nối việc chọn tab với đổi trang. Khi gỡ View của host:

```kotlin
tabs.setupWithViewPager(null)
```

Đây là API cho ViewPager, không nhận ViewPager2. [Nguồn: Swipe views với ViewPager](https://developer.android.com/guide/navigation/advanced/swipe-view).

### 4.2 ViewPager2: TabLayoutMediator

```kotlin
pager.adapter = adapter
val mediator = TabLayoutMediator(tabs, pager) { tab, position ->
    tab.text = titles[position]
}
mediator.attach()
```

Các biến trên là minh họa kết nối, chưa phải một class đầy đủ. Mediator đăng ký callback và theo dõi adapter để cập nhật các tab. Trình tự đúng là **set adapter → tạo mediator → attach**. Gọi `attach()` khi chưa có adapter hoặc attach hai lần mà chưa detach có thể ném exception.

Khi View bị hủy, gọi `detach()`. Nếu thay adapter, detach rồi attach lại để mediator theo dõi adapter mới. Nếu thay đối tượng TabLayout/ViewPager2, tạo mediator cho cặp View mới. Tab configuration có thể chạy lại khi dữ liệu thay đổi nên phải dựng lại text, badge và custom view từ model. [Nguồn: TabLayoutMediator API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayoutMediator).

Không cần thêm listener tự viết để nối tab → trang và trang → tab khi đã dùng mediator. Listener riêng chỉ phục vụ công việc bổ sung như render selected state hoặc ghi nhận tương tác.

## 5. Change page và page-change listener

### 5.1 Ba cách điều hướng và chỉ số trang

Người dùng có thể vuốt, bấm tab, hoặc bấm nút gọi code. `position` và `currentItem` dùng chỉ số từ **0**; UI “Trang 1/3” hiển thị `currentItem + 1`.

```kotlin
// Cả ViewPager và ViewPager2 đều có API này.
pager.setCurrentItem(1, true)  // Chuyển đến trang thứ hai, có cuộn mượt.
pager.setCurrentItem(0, false) // Chuyển ngay đến trang đầu.
```

Trong app, kiểm tra giới hạn trước khi đổi trang:

```kotlin
fun moveTo(pager: ViewPager2, target: Int) {
    val count = pager.adapter?.itemCount ?: 0
    if (target in 0 until count) {
        pager.setCurrentItem(target, true)
    }
}
```

ViewPager2 có thể bỏ qua lệnh khi không có dữ liệu và điều chỉnh index về giới hạn hợp lệ; UI vẫn nên tự kiểm tra để nút Trước/Sau có trạng thái đúng. `isUserInputEnabled = false` tắt thao tác cuộn của người dùng theo cơ chế pager nhưng không khóa mọi đường điều hướng: lệnh `setCurrentItem()` và chọn tab qua mediator vẫn đổi trang; API cũng ghi hạn chế với keyboard input. [Nguồn: ViewPager2 API](https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2).

Với form cần validation, không coi việc tắt vuốt là hoàn thành quy tắc điều hướng. Cần kiểm soát cả nút, tab và các đường đổi trang khác.

### 5.2 Ba callback cần phân biệt

| Callback | Ý nghĩa | Việc phù hợp |
| --- | --- | --- |
| `onPageScrolled(position, offset, offsetPixels)` | Vị trí trong quá trình cuộn; offset là tỷ lệ giữa các trang | Animation nhẹ theo tiến trình cuộn |
| `onPageSelected(position)` | Trang được chọn đã thay đổi | Cập nhật tiêu đề, Trước/Sau, state trang được chọn |
| `onPageScrollStateChanged(state)` | Trạng thái IDLE/DRAGGING/SETTLING | Biết pager đang đứng yên, kéo hoặc tự hoàn tất cuộn |

`onPageSelected()` không đồng nghĩa animation đã xong. `onPageScrolled()` không phải progress tải dữ liệu; `position` trong lúc cuộn không luôn là trang cuối cùng sẽ được chọn. Khi bấm tab hoặc đổi trang bằng code, luồng callback không nhất thiết giống thao tác kéo bằng tay; không hardcode một chuỗi duy nhất cho mọi tình huống.

```text
Ví dụ vuốt bằng tay:
IDLE → DRAGGING → SETTLING → IDLE
         └─ onPageScrolled có thể chạy nhiều lần ─┘
onPageSelected được gọi khi lựa chọn trang thay đổi,
không phải một mốc bảo đảm “mọi animation đã kết thúc”.
```

[Nguồn: ViewPager2.OnPageChangeCallback](https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2.OnPageChangeCallback), [ViewPager.OnPageChangeListener](https://developer.android.com/reference/androidx/viewpager/widget/ViewPager.OnPageChangeListener).

### 5.3 Đăng ký và gỡ listener

```kotlin
// ViewPager2
val callback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageSelected(position: Int) {
        // Render UI theo position hoặc lấy page ID từ adapter.
    }
}
pager.registerOnPageChangeCallback(callback)
// Khi View của Fragment host bị hủy:
pager.unregisterOnPageChangeCallback(callback)
```

```kotlin
// ViewPager
val listener = object : ViewPager.SimpleOnPageChangeListener() {
    override fun onPageSelected(position: Int) {
        // Cập nhật UI.
    }
}
pager.addOnPageChangeListener(listener)
// Khi View của Fragment host bị hủy:
pager.removeOnPageChangeListener(listener)
```

Giữ cùng instance để gỡ; tạo một callback mới ở lệnh unregister không gỡ được callback đã đăng ký. Không chạy truy vấn DB/network, xử lý bitmap hay log lớn trong mỗi frame của `onPageScrolled()`.

Nếu theo dõi lượt xem sau khi trang đứng yên, kết hợp page ID với trạng thái IDLE và chống ghi nhận trùng. Cần quyết định rõ yêu cầu: “chọn trang” khác “người dùng thực sự nhìn thấy trang khi app đang foreground”.

## 6. Custom UI TabLayout

### 6.1 Tùy biến bằng thuộc tính

| Nhu cầu | Thuộc tính/API |
| --- | --- |
| Vài tab, tên ngắn | `tabMode="fixed"`, cân nhắc `tabGravity="fill"` |
| Nhiều tab hoặc tên dài | `tabMode="scrollable"` |
| Màu chữ | `tabTextColor`, `tabSelectedTextColor` |
| Indicator | `tabIndicatorColor`, `tabIndicatorHeight`, `tabIndicatorFullWidth` |
| Phản hồi chạm | `tabRippleColor` |
| Icon | `tab.setIcon(...)` |
| Badge chuẩn Material | `tab.orCreateBadge`, `tab.removeBadge()` |
| Bố cục riêng | `tab.setCustomView(...)` |

Ví dụ chỉ đổi màu, icon hoặc badge thường chưa cần custom layout. TabLayout vẫn được hiển thị ngang kể cả khi nội dung ViewPager2 cuộn dọc. [Nguồn: TabLayout API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout.html), [Material Tabs guide](https://github.com/material-components/material-components-android/blob/master/docs/components/Tabs.md).

### 6.2 Custom view cần bind và selected state

Trong tab custom, nếu dùng TextView ID `android.R.id.text1` và ImageView ID `android.R.id.icon`, TabLayout có cơ chế cập nhật text/icon tương ứng. Nếu dùng ID riêng như ví dụ bên dưới, tự bind nội dung và style.

Custom tab cần có label đọc được, trạng thái selected rõ và vùng chạm đủ lớn. Tránh để TextView/badge con tự xử lý click, làm mất việc chọn tab của TabLayout. `contentDescription` nên bổ sung ý nghĩa của badge, không chỉ đọc một con số rời rạc. [Nguồn: TabLayout.Tab API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout.Tab).

## 7. Thực hành chính: ViewPager2 + Fragment + custom tab

Ứng dụng có ba trang: Tổng quan, Bài tập, Kết quả. Mỗi trang có bộ đếm để quan sát state khi chuyển trang và xoay máy. Tab có badge minh họa; nút Trước/Sau và vuốt phải đồng bộ với tab.

### 7.1 Chuẩn bị project

Trong một ứng dụng Views dùng Kotlin, cần các artifact tương thích với cấu hình dự án:

- `androidx.appcompat:appcompat`.
- `androidx.fragment:fragment-ktx`.
- `androidx.viewpager2:viewpager2`.
- `com.google.android.material:material`.
- `androidx.viewpager:viewpager` nếu chạy ví dụ cũ ở phần 8.

Chọn version theo version catalog hoặc quy ước dự án; không dùng chuỗi version giả như `latest`. Activity cần được khai báo trong manifest và dùng theme kế thừa `Theme.Material3.DayNight.NoActionBar` hoặc theme Material Components tương thích của project.

Ví dụ theme tối thiểu, `res/values/styles.xml`:

```xml
<resources>
    <style name="Theme.PagerLesson" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

Đặt `android:theme="@style/Theme.PagerLesson"` trên application hoặc Activity demo trong manifest hiện có. Các snippet không thay thế cấu hình Gradle/manifest của một app hoàn chỉnh.

### 7.2 Resource chung

`res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Pager Lesson</string>
    <string name="title_overview">Tổng quan</string>
    <string name="title_practice">Bài tập</string>
    <string name="title_result">Kết quả</string>
    <string name="body_overview">Tìm hiểu adapter, tab và vòng đời trang.</string>
    <string name="body_practice">Thực hành đổi trang và cập nhật selected state.</string>
    <string name="body_result">Kiểm tra state khi xoay máy và quay lại màn hình.</string>
    <string name="previous_page">Trước</string>
    <string name="next_page">Sau</string>
    <string name="increment_counter">Tăng bộ đếm</string>
    <string name="counter_value">Bộ đếm: %1$d</string>
    <string name="page_status">Trang %1$d/%2$d</string>
    <string name="tab_description">%1$s, %2$d mục mới</string>
</resources>
```

`res/color/tab_label_colors.xml` — selector dùng cho TextView trong custom tab:

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true" android:color="#1565C0" />
    <item android:color="#424242" />
</selector>
```

`res/drawable/bg_tab_badge.xml`:

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#B3261E" />
    <corners android:radius="12dp" />
</shape>
```

Màu ở đây nhằm giúp nhìn rõ demo. Trong sản phẩm, dùng theme/color token có hỗ trợ dark mode và kiểm tra độ tương phản.

### 7.3 Layout host và tab

`res/layout/fragment_pager_demo.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tabs"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:tabMode="fixed"
        app:tabGravity="fill"
        app:tabIndicatorColor="#1565C0"
        app:tabIndicatorHeight="3dp"
        app:tabIndicatorFullWidth="false" />

    <TextView
        android:id="@+id/pageStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:padding="8dp" />

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/pager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">

        <Button
            android:id="@+id/previousButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/previous_page" />

        <Button
            android:id="@+id/nextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/next_page" />
    </LinearLayout>
</LinearLayout>
```

`res/layout/view_custom_tab.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:minHeight="48dp"
    android:gravity="center"
    android:orientation="horizontal"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:clickable="false"
    android:focusable="false">

    <TextView
        android:id="@+id/tabLabel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@color/tab_label_colors"
        android:maxLines="1"
        android:textSize="14sp" />

    <TextView
        android:id="@+id/tabBadge"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="4dp"
        android:minWidth="20dp"
        android:gravity="center"
        android:paddingStart="4dp"
        android:paddingEnd="4dp"
        android:background="@drawable/bg_tab_badge"
        android:textColor="#FFFFFF"
        android:textSize="12sp"
        android:importantForAccessibility="no" />
</LinearLayout>
```

### 7.4 Model và adapter Fragment

`PageSpec.kt`:

```kotlin
package com.example.pagerlesson

import androidx.annotation.StringRes

data class PageSpec(
    val id: Long,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val badgeCount: Int = 0
)

fun lessonPages(): List<PageSpec> = listOf(
    PageSpec(11L, R.string.title_overview, R.string.body_overview, 3),
    PageSpec(22L, R.string.title_practice, R.string.body_practice),
    PageSpec(33L, R.string.title_result, R.string.body_result, 2)
)
```

ID đại diện cho trang; `11L` không phải index 11. Badge là dữ liệu minh họa cố định, chưa có nghiệp vụ đánh dấu đã đọc.

`LessonPagesAdapter.kt`:

```kotlin
package com.example.pagerlesson

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class LessonPagesAdapter(
    host: Fragment,
    private val pages: List<PageSpec>
) : FragmentStateAdapter(host) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment =
        LessonPageFragment.newInstance(pages[position])

    override fun getItemId(position: Int): Long = pages[position].id

    override fun containsItem(itemId: Long): Boolean =
        pages.any { it.id == itemId }
}
```

Mẫu tĩnh vẫn khai báo ID để nhìn rõ mối quan hệ trang–state. Với danh sách cố định không thêm/xóa/di chuyển, implementation ID mặc định của FragmentStateAdapter có thể đủ; danh sách động cần thiết kế ở phần 10.

### 7.5 Fragment nội dung với bộ đếm

`res/layout/fragment_lesson_page.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:id="@+id/pageBody"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:textSize="18sp" />

    <TextView
        android:id="@+id/counterText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp" />

    <Button
        android:id="@+id/incrementButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/increment_counter" />
</LinearLayout>
```

`LessonPageFragment.kt`:

```kotlin
package com.example.pagerlesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class LessonPageFragment : Fragment(R.layout.fragment_lesson_page) {

    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        counter = savedInstanceState?.getInt(STATE_COUNTER) ?: 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val body = view.findViewById<TextView>(R.id.pageBody)
        val counterText = view.findViewById<TextView>(R.id.counterText)
        val increment = view.findViewById<Button>(R.id.incrementButton)
        body.setText(requireArguments().getInt(ARG_BODY))

        fun renderCounter() {
            counterText.text = getString(R.string.counter_value, counter)
        }
        renderCounter()
        increment.setOnClickListener {
            counter++
            renderCounter()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_COUNTER, counter)
    }

    companion object {
        private const val ARG_PAGE_ID = "page_id"
        private const val ARG_BODY = "body_res"
        private const val STATE_COUNTER = "counter"

        fun newInstance(page: PageSpec) = LessonPageFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PAGE_ID, page.id)
                putInt(ARG_BODY, page.bodyRes)
            }
        }
    }
}
```

Fragment có constructor không tham số để framework tạo lại. Arguments chỉ mang dữ liệu nhỏ; app thực tế thường truyền ID rồi đọc dữ liệu từ ViewModel/repository. Bộ đếm minh họa saved state của Fragment, không thay thế lưu dữ liệu nghiệp vụ lâu dài.

### 7.6 Host: gắn adapter, custom tab, nút và callback

`PagerDemoFragment.kt`:

```kotlin
package com.example.pagerlesson

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PagerDemoFragment : Fragment(R.layout.fragment_pager_demo) {

    private var pager: ViewPager2? = null
    private var tabs: TabLayout? = null
    private var mediator: TabLayoutMediator? = null
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null
    private var tabListener: TabLayout.OnTabSelectedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pagerView = view.findViewById<ViewPager2>(R.id.pager)
        val tabView = view.findViewById<TabLayout>(R.id.tabs)
        val status = view.findViewById<TextView>(R.id.pageStatus)
        val previous = view.findViewById<Button>(R.id.previousButton)
        val next = view.findViewById<Button>(R.id.nextButton)
        val pages = lessonPages()
        val adapter = LessonPagesAdapter(this, pages)

        pager = pagerView
        tabs = tabView
        pagerView.adapter = adapter

        fun renderPage(position: Int) {
            status.text = getString(R.string.page_status, position + 1, adapter.itemCount)
            previous.isEnabled = position > 0
            next.isEnabled = position < adapter.itemCount - 1
        }

        fun moveTo(target: Int) {
            if (target in 0 until adapter.itemCount) {
                pagerView.setCurrentItem(target, true)
            }
        }

        previous.setOnClickListener { moveTo(pagerView.currentItem - 1) }
        next.setOnClickListener { moveTo(pagerView.currentItem + 1) }

        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderPage(position)
                Log.d("PagerLesson", "selected id=${pages[position].id}")
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // Chỉ thêm animation nhẹ nếu bài tập cần.
            }

            override fun onPageScrollStateChanged(state: Int) {
                Log.d("PagerLesson", "scrollState=$state")
            }
        }
        pageCallback = callback
        pagerView.registerOnPageChangeCallback(callback)

        val selectionListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                renderTabSelection(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                renderTabSelection(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Bấm lại tab hiện tại không tạo một trang mới.
            }
        }
        tabListener = selectionListener
        tabView.addOnTabSelectedListener(selectionListener)

        mediator = TabLayoutMediator(tabView, pagerView) { tab, position ->
            val page = pages[position]
            val title = getString(page.titleRes)
            tab.text = title
            tab.contentDescription = if (page.badgeCount > 0) {
                getString(R.string.tab_description, title, page.badgeCount)
            } else {
                title
            }
            tab.setCustomView(R.layout.view_custom_tab)
            val custom = requireNotNull(tab.customView)
            custom.findViewById<TextView>(R.id.tabLabel).text = title
            custom.findViewById<TextView>(R.id.tabBadge).apply {
                text = page.badgeCount.toString()
                visibility = if (page.badgeCount > 0) View.VISIBLE else View.GONE
            }
            renderTabSelection(tab, false)
        }.also { it.attach() }

        renderPage(pagerView.currentItem)
        // Không setCurrentItem(0) vô điều kiện: framework còn có thể restore vị trí.
    }

    private fun renderTabSelection(tab: TabLayout.Tab, selected: Boolean) {
        val custom = tab.customView ?: return
        custom.isSelected = selected
        custom.findViewById<TextView>(R.id.tabLabel).apply {
            isSelected = selected
            setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    override fun onDestroyView() {
        mediator?.detach()
        mediator = null
        tabListener?.let { tabs?.removeOnTabSelectedListener(it) }
        pageCallback?.let { pager?.unregisterOnPageChangeCallback(it) }
        pager?.adapter = null
        tabListener = null
        pageCallback = null
        tabs = null
        pager = null
        super.onDestroyView()
    }
}
```

Listener tab chỉ đổi style custom; mediator đảm nhiệm liên kết trang. Các reference đến View, callback và mediator được giải phóng khi View host bị hủy. Lifecycle Fragment con vẫn do FragmentStateAdapter/FragmentManager quản lý; không tự remove từng page Fragment.

### 7.7 Activity để chạy ví dụ

`res/layout/activity_pager_lesson.xml`:

```xml
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/activityContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

`MainActivity.kt`:

```kotlin
package com.example.pagerlesson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_pager_lesson) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.activityContainer, PagerDemoFragment())
                .commit()
        }
    }
}
```

Không replace lại host khi Activity đang restore Fragment. Khi tích hợp app dùng edge-to-edge, xử lý system bar insets theo cơ chế của app để tab/nút không nằm dưới status/navigation bar.

## 8. Thực hành ViewPager cũ + PagerAdapter + TabLayout

Mẫu này dùng trang View, không dùng Fragment. Nó giúp thấy trách nhiệm `instantiateItem()`/`destroyItem()` mà Fragment adapter thường che đi.

### 8.1 Trang View dùng chung

`res/layout/page_simple.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:id="@+id/simpleTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textSize="20sp" />

    <TextView
        android:id="@+id/simpleBody"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:layout_marginTop="16dp" />
</LinearLayout>
```

`SimplePage.kt`:

```kotlin
package com.example.pagerlesson

data class SimplePage(val title: String, val body: String)
```

`ViewPagesAdapter.kt`:

```kotlin
package com.example.pagerlesson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter

class ViewPagesAdapter(private val pages: List<SimplePage>) : PagerAdapter() {

    override fun getCount(): Int = pages.size

    override fun getPageTitle(position: Int): CharSequence = pages[position].title

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val root = LayoutInflater.from(container.context)
            .inflate(R.layout.page_simple, container, false)
        val page = pages[position]
        root.findViewById<TextView>(R.id.simpleTitle).text = page.title
        root.findViewById<TextView>(R.id.simpleBody).text = page.body
        container.addView(root)
        return root
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean =
        view === `object`

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
```

Mẫu tĩnh này không có input cần lưu. Nếu trang View chứa input, cần thiết kế state theo page ID và lưu/restore; không coi việc còn View trong bộ nhớ là cơ chế lưu state lâu dài.

### 8.2 Layout và Fragment host

`res/layout/fragment_legacy_pager.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/legacyTabs"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:tabMode="fixed" />

    <TextView
        android:id="@+id/legacyStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center" />

    <androidx.viewpager.widget.ViewPager
        android:id="@+id/legacyPager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
</LinearLayout>
```

`LegacyPagerFragment.kt`:

```kotlin
package com.example.pagerlesson

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

class LegacyPagerFragment : Fragment(R.layout.fragment_legacy_pager) {
    private var pager: ViewPager? = null
    private var tabs: TabLayout? = null
    private var pageListener: ViewPager.OnPageChangeListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pagerView = view.findViewById<ViewPager>(R.id.legacyPager)
        val tabView = view.findViewById<TabLayout>(R.id.legacyTabs)
        val status = view.findViewById<TextView>(R.id.legacyStatus)
        val pages = lessonPages().map {
            SimplePage(getString(it.titleRes), getString(it.bodyRes))
        }
        val adapter = ViewPagesAdapter(pages)
        pager = pagerView
        tabs = tabView
        pagerView.adapter = adapter
        tabView.setupWithViewPager(pagerView)

        fun render(position: Int) {
            status.text = getString(R.string.page_status, position + 1, adapter.count)
        }

        val listener = object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                render(position)
            }
        }
        pageListener = listener
        pagerView.addOnPageChangeListener(listener)
        render(pagerView.currentItem)
    }

    override fun onDestroyView() {
        tabs?.setupWithViewPager(null)
        pageListener?.let { pager?.removeOnPageChangeListener(it) }
        pager?.adapter = null
        pageListener = null
        tabs = null
        pager = null
        super.onDestroyView()
    }
}
```

Để chạy mẫu này, đổi Fragment khởi tạo trong MainActivity thành `LegacyPagerFragment()`. Vuốt và bấm tab phải đồng bộ; có thể thêm Trước/Sau với cùng quy tắc bounds ở phần 5.

### 8.3 Adapter Fragment cũ: đọc hiểu và migration

Ví dụ sau chỉ phục vụ bảo trì; cần file `OldFragmentsAdapter.kt` nếu muốn thử nó:

```kotlin
package com.example.pagerlesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

@Suppress("DEPRECATION")
class OldFragmentsAdapter(
    manager: FragmentManager,
    private val pages: List<PageSpec>,
    private val titles: List<String>
) : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    init {
        require(pages.size == titles.size)
    }

    override fun getCount(): Int = pages.size
    override fun getItem(position: Int): Fragment =
        LessonPageFragment.newInstance(pages[position])
    override fun getPageTitle(position: Int): CharSequence = titles[position]
}
```

Trong Fragment host của ViewPager cũ, dùng `OldFragmentsAdapter(childFragmentManager, pages, titles)`. `BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT` giới hạn Fragment ngoài trang hiện tại ở STARTED; tránh tiếp tục xây dựng logic mới dựa trên `setUserVisibleHint()`.

Đổi base class thành FragmentPagerAdapter không chỉ là đổi tên: chiến lược giữ Fragment instance khác nhau. Khi chuyển sang ViewPager2, đổi cả adapter, hàm tạo trang, API callback và cách kết nối tab. [Nguồn: Migration guide](https://developer.android.com/develop/ui/views/animations/vp2-migration).

## 9. ViewPager2 với trang View: RecyclerView.Adapter

Trang chỉ hiển thị ảnh/text đơn giản có thể dùng ViewHolder, không bắt buộc tạo Fragment. Mẫu dưới tái sử dụng `SimplePage` và `page_simple.xml` ở phần 8.

`SimplePagesAdapter.kt`:

```kotlin
package com.example.pagerlesson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimplePagesAdapter(
    private val pages: List<SimplePage>
) : RecyclerView.Adapter<SimplePagesAdapter.PageHolder>() {

    class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.simpleTitle)
        private val body = view.findViewById<TextView>(R.id.simpleBody)

        fun bind(page: SimplePage) {
            title.text = page.title
            body.text = page.body
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_simple, parent, false)
        return PageHolder(root)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}
```

**Root của một trang ViewPager2 phải có width và height `match_parent`** theo yêu cầu page layout của widget. Việc đặt ViewPager2 trong host với height `0dp` + weight không mâu thuẫn: host đo pager trước, còn mỗi page lấp đầy kích thước pager.

Trong `onViewCreated()` của host ViewPager2, có thể thay adapter Fragment bằng adapter trên và vẫn dùng mediator/callback tương ứng. Danh sách động lớn của View pages có thể dùng ListAdapter + DiffUtil; state của input cần gắn với item ID giống bài RecyclerView. [Nguồn: Migration guide](https://developer.android.com/develop/ui/views/animations/vp2-migration).

## 10. Trang động, stable ID và khôi phục lựa chọn

Phần này là ví dụ mở rộng độc lập, không thay đổi dataset cố định của demo chính. Giả sử server trả danh mục:

```text
Trước: [11 Tổng quan] [22 Bài tập] [33 Kết quả]
Sau:   [33 Kết quả]   [11 Tổng quan]
```

Trang ID 33 vẫn là Kết quả nhưng chuyển từ position 2 sang position 0. Nếu lấy position làm identity, Fragment/state có thể bị gắn sai trang. Với FragmentStateAdapter, override **cả getItemId và containsItem**. ID phải duy nhất, ổn định theo entity và không dùng `RecyclerView.NO_ID`. [Nguồn: FragmentStateAdapter API](https://developer.android.com/reference/androidx/viewpager2/adapter/FragmentStateAdapter).

`DynamicPagesAdapter.kt` — ví dụ danh sách nhỏ:

```kotlin
package com.example.pagerlesson

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DynamicPagesAdapter(
    host: Fragment,
    initial: List<PageSpec>
) : FragmentStateAdapter(host) {
    private var pages = initial.toList()

    init {
        validate(pages)
    }

    override fun getItemCount(): Int = pages.size
    override fun createFragment(position: Int): Fragment =
        LessonPageFragment.newInstance(pages[position])
    override fun getItemId(position: Int): Long = pages[position].id
    override fun containsItem(itemId: Long): Boolean = pages.any { it.id == itemId }

    fun pageAt(position: Int): PageSpec? = pages.getOrNull(position)
    fun indexOfId(id: Long): Int = pages.indexOfFirst { it.id == id }

    fun replacePages(next: List<PageSpec>) {
        validate(next)
        pages = next.toList()
        notifyDataSetChanged()
    }

    private fun validate(items: List<PageSpec>) {
        require(items.all { it.id >= 0L })
        require(items.map { it.id }.distinct().size == items.size)
    }
}
```

Việc giới hạn ID không âm là quy ước của demo. Với dataset lớn, cân nhắc notification/diff phù hợp; không cập nhật adapter ở mỗi frame cuộn.

Đoạn tích hợp dưới chạy trên main thread khi có snapshot mới; `pager` và `adapter` là biến ViewPager2/DynamicPagesAdapter đã thiết lập trong host:

```kotlin
val selectedId = adapter.pageAt(pager.currentItem)?.id
adapter.replacePages(nextPages)

if (adapter.itemCount > 0) {
    val samePageIndex = selectedId?.let(adapter::indexOfId) ?: -1
    val target = if (samePageIndex >= 0) samePageIndex else 0
    pager.setCurrentItem(target, false)
} else {
    // Render empty state và disable điều hướng ở host.
}
```

Nếu trang hiện tại đã bị xóa, quy tắc chọn trang đầu chỉ là lựa chọn demo; sản phẩm có thể chọn trang gần nhất. Stable ID giữ identity/state Fragment, không tự định nghĩa trải nghiệm chọn trang sau mọi lần reorder.

Khi tạo mediator cho adapter động, lấy title/badge từ `adapter.pageAt(position)` ở mỗi lần configuration chạy, không đọc list cũ được capture lúc khởi tạo. Với custom tab, reapply selected style cho mọi tab theo `tabs.selectedTabPosition` sau khi mediator tái tạo tab; không dựa vào việc listener luôn phát lại cho cùng vị trí.

Các điểm dễ sai:

- Cùng ID nhưng đổi `bodyRes` không làm arguments của Fragment đã tồn tại tự cập nhật. Dữ liệu thay đổi của cùng entity nên observable từ ViewModel/repository.
- Không đổi ID mỗi lần refresh để ép tạo lại Fragment: sẽ mất ý nghĩa identity/state.
- Snapshot badge phải nằm trong model; tab có thể được mediator tạo lại.
- Khi data đến bất đồng bộ sau recreation, phối hợp restore lựa chọn theo ID bằng SavedStateHandle/saved state và dữ liệu sẵn sàng. Không ghi đè lựa chọn về 0 ở mọi lần emission.
- Tránh sửa dataset giữa tương tác đang cuộn nếu nghiệp vụ không cần; xác định cách xử lý để không nhảy trang khó hiểu.

## 11. Offscreen page, orientation và gesture

### 11.1 Không tăng offscreenPageLimit để chữa mất state

ViewPager2 mặc định dùng `OFFSCREEN_PAGE_LIMIT_DEFAULT` (-1), dựa vào cơ chế caching của RecyclerView. Giá trị custom phải từ 1 trở lên; 0 không phải cách tắt cache hợp lệ. Tăng giới hạn có thể giảm việc tạo lại trang nhưng tăng View, Fragment, ảnh và bộ nhớ được giữ.

State đúng phải đến từ identity, saved state và ViewModel/repository. Không đặt limit bằng toàn bộ số trang chỉ để né lỗi bộ đếm bị reset. [Nguồn: ViewPager2 API](https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2).

### 11.2 Đổi hướng và RTL

Đoạn cấu hình trong host:

```kotlin
pager.orientation = ViewPager2.ORIENTATION_VERTICAL
// Hoặc ViewPager2.ORIENTATION_HORIZONTAL.
```

TabLayout vẫn là thanh tab ngang. Kiểm tra vùng nội dung và thao tác khi dùng pager dọc. Khi thử RTL, “trang trước/sau” trong code vẫn theo index dataset; không tự đảo list chỉ vì hướng đọc đổi.

### 11.3 Nội dung con cuộn cùng hướng với pager

Ví dụ pager ngang chứa gallery/RecyclerView ngang hoặc ảnh có pan ngang: parent và child cùng muốn xử lý gesture. ViewPager2 không có giải pháp mặc định cho mọi tình huống nested scrolling cùng hướng.

Giải pháp có thể dùng wrapper và `requestDisallowInterceptTouchEvent()` theo hướng gesture cùng khả năng cuộn của child. Không chặn parent vô điều kiện: lúc child chạm biên, người dùng vẫn có thể cần chuyển page. Đọc sample nested-scroll chính thức được dẫn trong [Migration guide](https://developer.android.com/develop/ui/views/animations/vp2-migration) và đối chiếu lại bài Touch/ViewGroup trước khi sửa flow.

Pager ngang chứa RecyclerView dọc thường dễ phân biệt hướng hơn, nhưng vẫn cần test kéo chéo, fling, accessibility và thiết bị thật.

## 12. Lỗi thường gặp và cách sửa

| Hiện tượng | Nguyên nhân cần kiểm tra | Hướng sửa |
| --- | --- | --- |
| Không gọi được setupWithViewPager với ViewPager2 | Dùng API của ViewPager cũ | Dùng TabLayoutMediator |
| Tab không có tên ở ViewPager | Adapter không trả page title | Override getPageTitle |
| Mediator attach ném exception | Chưa set adapter hoặc attach trùng | Set adapter trước; detach trước khi attach lại |
| Custom tab có màu cũ | Chưa bind selected state hoặc dùng ID riêng mà chờ tự bind | Bind label/badge; cập nhật selected/unselected |
| Vuốt rồi tab nhảy qua lại | Hai listener tự gọi đổi trang/chọn tab lẫn nhau | Để mediator/setup quản lý kết nối; listener riêng chỉ render |
| Fragment child bị lỗi restore | Dùng manager của Activity cho pager nằm trong Fragment | Chọn đúng host/childFragmentManager |
| State chuyển nhầm danh mục sau reorder | Dùng position làm ID | Override getItemId + containsItem; ID ổn định |
| Xoay máy trở về trang đầu | App luôn setCurrentItem(0) hoặc replace lại host | Cho restore chạy; chỉ khởi tạo host lần đầu |
| Callback chạy nhiều lần sau quay lại | Đăng ký thêm mà không gỡ instance cũ | Cleanup ở onDestroyView |
| Tab bị cắt chữ | Fixed mode quá nhiều tab/tên dài/custom layout rộng | Dùng scrollable; kiểm tra font scale và bản dịch |
| Trang ViewPager2 gây lỗi kích thước | Root page không lấp đầy pager | Width/height page match_parent |
| Vuốt trong carousel không đổi trang đúng | Xung đột gesture cùng hướng | Điều phối intercept theo hướng và biên cuộn |

## 13. Bài tập và ma trận kiểm thử

### 13.1 Bài tập theo mức độ

**Bài 1 — Lựa chọn và adapter:** với gallery 20 ảnh, màn hình 3 tab Fragment và feed 5.000 item, chọn thành phần/adapter phù hợp. Giải thích lifecycle và state của mỗi lựa chọn.

**Bài 2 — Demo chính:** triển khai phần 7. Vuốt, bấm tab, Trước/Sau phải cập nhật cùng lựa chọn. Mỗi trang có bộ đếm riêng; xoay máy giữ trang và bộ đếm.

**Bài 3 — Custom UI:** thay badge minh họa bằng model unread; đổi sang scrollable khi có 8 tab. Thêm icon bằng Drawable có sẵn của app hoặc VectorDrawable; kiểm tra TalkBack, cỡ chữ lớn và dark mode.

**Bài 4 — Migration:** chạy phần 8 bằng ViewPager, sau đó chuyển sang ViewPager2 với SimplePagesAdapter. Ghi lại thay đổi adapter, title configuration, listener và cleanup.

**Bài 5 — Trang động:** thêm/xóa/reorder danh mục, giữ trang theo ID; xóa trang đang chọn theo quy tắc đã thống nhất. Thêm empty state và restore sau khi data đến chậm.

### 13.2 Ma trận kiểm thử thủ công

| Thao tác | Kết quả cần thấy |
| --- | --- |
| Mở màn hình lần đầu | Một tab selected, chỉ số 1/3, Trước disabled |
| Vuốt sang trang 2 | Tab 2 selected; chỉ số/nút đồng bộ |
| Bấm tab 3 | Nội dung trang 3 hiện; Sau disabled |
| Bấm Trước liên tiếp | Không vượt bounds; không crash khi còn animation |
| Bấm lại tab hiện tại | Không tạo lại Fragment hoặc reset bộ đếm |
| Đổi trang có/không smooth scroll | Callback và trạng thái cuối đúng, không giả định DRAGGING luôn xuất hiện |
| Tăng bộ đếm ở hai trang, chuyển qua lại | Bộ đếm thuộc đúng trang |
| Xoay máy khi ở trang 2 | Host không bị thêm trùng; trang/state được restore |
| Back khỏi host rồi mở lại | Không có callback cũ giữ View; lần vào mới theo yêu cầu sản phẩm |
| App background rồi foreground | Lifecycle trang theo host; không ghi lượt xem chỉ vì có selected index |
| Tắt isUserInputEnabled | Vuốt bị hạn chế, tab/code vẫn có thể điều hướng theo thiết kế |
| Thêm/xóa/reorder page ID | State gắn đúng entity; lựa chọn theo ID hoặc fallback đã định nghĩa |
| Dataset rỗng rồi có dữ liệu | Empty state đúng; không hiển thị “Trang 1/0” |
| Đổi badge rồi mediator refresh | Badge/contentDescription/selected style đúng |
| Font scale lớn, tên tab dài, RTL | Tab đọc được và chọn đúng; không đảo dữ liệu tùy tiện |
| TalkBack | Tab có tên và trạng thái chọn; badge có ngữ nghĩa |
| Pager chứa list cùng hướng | Child cuộn được; page đổi theo gesture/biên cuộn thiết kế |

Khi đánh giá process recreation, dùng luồng đưa app về background rồi tái tạo process trong môi trường debug và mở lại task; phân biệt với force-stop hoặc xóa task, vốn có ý nghĩa khởi động khác. Đây là kiểm tra bổ sung sau khi build được app, không phải kết quả đã xác nhận trong workspace tài liệu.

### 13.3 Rubric đánh giá intern

| Hạng mục | Điểm |
| --- | ---: |
| Chọn use case và phân biệt ViewPager/ViewPager2 | 10 |
| Giải thích các adapter, create/reuse và Fragment lifecycle | 20 |
| Tích hợp cả hai pager với TabLayout đúng API | 20 |
| Custom tab có selected state và accessibility | 15 |
| Change page, bounds, callback và cleanup | 15 |
| State, stable ID, xoay máy và trang động | 15 |
| Trình bày bằng demo/log và giải thích lỗi | 5 |
| **Tổng** | **100** |

Mức gợi ý đạt: từ 75 điểm, đồng thời không còn lỗi crash, sai manager, callback giữ View cũ hoặc state gắn nhầm trang. Điểm số không thay thế việc intern tự giải thích được code.

## 14. Câu hỏi ôn tập và đáp án ngắn

1. **TabLayout có tạo Fragment không?** Không; adapter tạo trang, cơ chế kết nối đồng bộ lựa chọn tab–pager.
2. **ViewPager2 dùng PagerAdapter được không?** Không; dùng RecyclerView.Adapter hoặc FragmentStateAdapter.
3. **FragmentPagerAdapter giữ gì?** Giữ Fragment instance theo chiến lược cũ; không bảo đảm mọi View của Fragment còn tồn tại.
4. **Vì sao createFragment trả instance mới?** Adapter/FragmentManager quản lý instance và restore; cache thủ công làm sai ownership/state.
5. **Pager ở Fragment dùng manager nào?** Fragment con thuộc childFragmentManager; constructor FragmentStateAdapter(host) chọn phạm vi đó.
6. **onPageSelected có nghĩa animation đã xong không?** Không; quan sát IDLE khi cần mốc hết cuộn và kết hợp điều kiện khác theo nghiệp vụ.
7. **Chỉ tắt vuốt có ngăn bỏ qua validation không?** Không; tab và code vẫn có thể đổi trang.
8. **Vì sao stable ID không nên bằng position?** Vị trí đổi khi danh sách thêm/xóa/reorder, identity của entity không đổi.
9. **Tăng offscreen limit có phải lưu state?** Không; chỉ thay lượng trang được giữ và chi phí bộ nhớ.
10. **Sau data refresh custom tab có thể mất badge vì sao?** Mediator có thể dựng lại tab; binder phải đọc model hiện tại.

## 15. Checklist hoàn thành

- [ ] Chọn được use case dùng pager và trường hợp nên dùng list/Navigation.
- [ ] Phân biệt PagerAdapter, hai Fragment adapter cũ, RecyclerView.Adapter và FragmentStateAdapter.
- [ ] Tạo page bằng View và Fragment; không cache Fragment tùy tiện.
- [ ] Dùng đúng host, FragmentManager và lifecycle.
- [ ] Ghép ViewPager bằng setupWithViewPager; ViewPager2 bằng mediator.
- [ ] Bind title, icon/badge/custom tab và selected state.
- [ ] Đổi trang bằng tab/vuốt/code; kiểm tra bounds và chỉ số hiển thị.
- [ ] Giải thích ba callback và các trạng thái cuộn.
- [ ] Gỡ callback/listener/mediator khi View host bị hủy.
- [ ] Kiểm tra restore trang và state; dùng ID ổn định khi dataset động.
- [ ] Kiểm tra gesture, font scale, RTL, TalkBack và theme.

## 16. Nguồn tài liệu và cách dùng

Các giải thích, bài tập và demo trong tài liệu được biên soạn cho intern. Nguồn đối chiếu là tài liệu chính thức và mã nguồn AndroidX/Material; ví dụ cần được build/test trong project thực tế trước khi dùng làm code sản phẩm.

| Nguồn | Dùng để đối chiếu |
| --- | --- |
| [Swipe views với ViewPager2](https://developer.android.com/guide/navigation/advanced/swipe-view-2) | Fragment pages và kết nối tab |
| [Swipe views với ViewPager](https://developer.android.com/guide/navigation/advanced/swipe-view) | Flow ViewPager cũ và page title |
| [Migration guide](https://developer.android.com/develop/ui/views/animations/vp2-migration) | Mapping adapter/API và nested gesture |
| [ViewPager2 API](https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2) | setCurrentItem, orientation, offscreen limit, user input |
| [ViewPager API](https://developer.android.com/reference/androidx/viewpager/widget/ViewPager) | Listener và điều hướng pager cũ |
| [FragmentStateAdapter API](https://developer.android.com/reference/androidx/viewpager2/adapter/FragmentStateAdapter) | Constructor, item identity và state |
| [FragmentPagerAdapter API](https://developer.android.com/reference/androidx/fragment/app/FragmentPagerAdapter) | Adapter cũ, deprecated và lifecycle behavior |
| [FragmentStatePagerAdapter source](https://github.com/androidx/androidx/blob/androidx-main/fragment/fragment/src/main/java/androidx/fragment/app/FragmentStatePagerAdapter.java) | Lưu state và hủy Fragment ngoài vùng cần thiết |
| [FragmentStateAdapter source](https://github.com/androidx/androidx/blob/androidx-main/viewpager2/viewpager2/src/main/java/androidx/viewpager2/adapter/FragmentStateAdapter.java) | Cách giới hạn lifecycle trang |
| [Page-change callback](https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2.OnPageChangeCallback) | Semantics của ba callback ViewPager2 |
| [Page-change listener](https://developer.android.com/reference/androidx/viewpager/widget/ViewPager.OnPageChangeListener) | Callback ViewPager |
| [TabLayout API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout.html) | Tab mode và thuộc tính UI |
| [TabLayout.Tab API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayout.Tab) | Custom view, icon, badge và content description |
| [TabLayoutMediator API](https://developer.android.com/reference/com/google/android/material/tabs/TabLayoutMediator) | attach/detach, adapter observer, configuration strategy |
| [Material Tabs guide](https://github.com/material-components/material-components-android/blob/master/docs/components/Tabs.md) | Styling và cách dùng tab |
| [ViewPager2 releases](https://developer.android.com/jetpack/androidx/releases/viewpager2) | Tình trạng bảo trì và thông tin phiên bản |
