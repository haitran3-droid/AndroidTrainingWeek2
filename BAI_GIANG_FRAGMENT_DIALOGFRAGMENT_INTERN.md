# Bài giảng Android: Fragment và DialogFragment — Level Intern

**Đối tượng:** intern đã biết Kotlin, Activity, View/ViewGroup và layout XML.  
**Phạm vi:** AndroidX Fragment, Kotlin + XML.  
**Thời lượng gợi ý:** 3 buổi × 3 giờ, cộng thời gian thực hành.  
**Ngày đối chiếu nguồn:** 06/10/2026.  
**Bài trước:** [View và ViewGroup](BAI_GIANG_VIEW_VIEWGROUP_INTERN.md).

Tài liệu dùng `androidx.fragment.app.Fragment`, `DialogFragment` và `FragmentManager`. Không trộn với các class `android.app.Fragment` cũ. `FragmentManger` trong yêu cầu được hiểu là `FragmentManager`.

Ví dụ có tên file có thể đưa vào một ứng dụng Android dùng Views. Thay package `com.example.fragmentlesson` bằng package thực tế; import `R` từ namespace ứng dụng nếu khác package. Cần dependency AndroidX Fragment và AppCompat tương thích với dự án, cùng theme AppCompat. Dùng phiên bản dependency được thống nhất trong dự án; tra [AndroidX Fragment releases](https://developer.android.com/jetpack/androidx/releases/fragment) khi chọn hoặc nâng phiên bản.

**Trạng thái kiểm chứng:** đã kiểm tra cấu trúc Markdown và cú pháp các khối XML. Ví dụ Kotlin chưa được biên dịch hoặc chạy trên thiết bị vì workspace hiện chưa có ứng dụng Android/Gradle; phần 11 cung cấp ma trận kiểm tra khi triển khai.

## 1. Mục tiêu và lộ trình

| Nội dung | Sau bài học, intern làm được |
| --- | --- |
| Fragment | Tạo màn hình, truyền arguments, xử lý View lifecycle |
| FragmentManager | Thực hiện transaction, tìm Fragment, hiểu back stack và restore |
| Các scope manager | Chọn đúng supportFragmentManager, parentFragmentManager, childFragmentManager |
| Nested Fragment | Đặt Fragment con vào container thuộc Fragment cha |
| DialogFragment | Tạo, hiển thị, đóng dialog; tránh tạo trùng sau restore |
| Giao tiếp | Gửi và nhận Fragment Result trên cùng manager |

| Buổi | Nội dung | Thực hành |
| --- | --- | --- |
| 1 | Fragment lifecycle, FragmentManager, transaction | Hai màn hình và Back |
| 2 | Parent/child manager, nested back stack | Dashboard chứa Summary/Detail |
| 3 | DialogFragment và Fragment Result | Dialog thuộc Dashboard, trả kết quả, thử xoay màn hình |

## 2. Fragment là gì?

Fragment là thành phần có lifecycle riêng, thường đại diện cho một phần UI của màn hình. Fragment cần host là Activity hoặc Fragment khác; không tự tồn tại như một Activity độc lập. `FragmentManager` quản lý việc gắn Fragment vào host và chuyển trạng thái. [Nguồn: Create a fragment](https://developer.android.com/guide/fragments/create).

```text
MainActivity
└── DashboardFragment
    ├── SummaryFragment hoặc DetailFragment
    └── ResetDialogFragment (khi dialog đang mở)
```

Fragment không phải View: một Fragment có thể tạo View và quản lý View đó, nhưng lifecycle của Fragment và lifecycle của View không hoàn toàn giống nhau.

### 2.1. Fragment lifecycle và View lifecycle

```text
Luồng callback thường gặp khi tạo Fragment có UI:
onAttach → onCreate → onCreateView → onViewCreated
         → onViewStateRestored → onStart → onResume

Khi rời UI:
onPause → onStop → onDestroyView

Khi Fragment bị hủy hoàn toàn:
onDestroy → onDetach
```

Đây là mô hình đơn giản để học, không phải cam kết mọi callback luôn xảy ra đúng một lần. Khi Fragment cũ được giữ để quay lại bằng back stack, View có thể bị hủy rồi tạo lại; Fragment chưa nhất thiết bị hủy hoàn toàn. Fragment con không thể vượt trạng thái lifecycle của Fragment cha. [Nguồn: Fragment lifecycle](https://developer.android.com/guide/fragments/lifecycle).

| Nơi xử lý | Việc phù hợp |
| --- | --- |
| `onCreate()` | Đọc arguments và state không phụ thuộc View |
| `onViewCreated()` | Tìm View, gắn listener, observe UI bằng viewLifecycleOwner |
| `onDestroyView()` | Xóa tham chiếu tới View/Binding của lần tạo UI này |
| `onSaveInstanceState()` | Lưu state nhỏ cần khôi phục |

Ví dụ nếu dùng ViewBinding: đặt `_binding = null` trong `onDestroyView()`. Không giữ binding của View cũ để cập nhật sau khi quay lại màn hình. `viewLifecycleOwner` chỉ dùng khi Fragment đã tạo View hợp lệ. [Nguồn: Fragment API](https://developer.android.com/reference/androidx/fragment/app/Fragment).

### 2.2. Arguments và khôi phục Fragment

Truyền dữ liệu ban đầu bằng `arguments`/`Bundle`; không dùng constructor như `DetailFragment(productId)` khi dựa vào FragmentFactory mặc định. Factory mặc định tạo lại Fragment bằng constructor không tham số. Dependency injection qua FragmentFactory là nội dung nâng cao. [Nguồn: Fragment manager](https://developer.android.com/guide/fragments/fragmentmanager).

```kotlin
// Fragment nhận arguments đọc trong onCreate(), ví dụ:
val itemId = requireArguments().getLong("item_id")

// Nơi điều hướng cung cấp arguments trước khi Fragment được thêm vào manager:
val args = android.os.Bundle().apply { putLong("item_id", 42L) }
parentFragmentManager.beginTransaction()
    .setReorderingAllowed(true)
    .replace(R.id.activityContainer, ProductDetailFragment::class.java, args)
    .addToBackStack("product_detail")
    .commit()
```

Đây là snippet minh họa: `ProductDetailFragment` và container tương ứng cần tồn tại. Container ở Activity phù hợp khi Fragment gọi đang là Fragment cấp Activity; phần 4 giải thích vì sao không áp dụng nguyên xi cho Fragment lồng nhau.

## 3. FragmentManager quản lý những gì?

`FragmentManager` chịu trách nhiệm quản lý các Fragment trong scope của nó: thêm/gỡ/thay thế, điều phối lifecycle, tìm Fragment, quản lý transaction/back stack và khôi phục state. `FragmentTransaction` mô tả một nhóm thay đổi; manager thực thi nhóm thay đổi đó. [Nguồn: Fragment manager](https://developer.android.com/guide/fragments/fragmentmanager).

### 3.1. Các thao tác cần nắm

| API | Ý nghĩa |
| --- | --- |
| `beginTransaction()` | Tạo transaction mới |
| `add()` | Thêm Fragment; không tự gỡ Fragment đang có trong container |
| `replace()` | Gỡ các Fragment hiện có trong cùng container thuộc manager này và thêm Fragment mới |
| `remove()` | Gỡ Fragment khỏi manager theo transaction |
| `show()` / `hide()` | Đổi hiển thị View; không tự chuyển lifecycle |
| `attach()` / `detach()` | Tạo lại/hủy View của Fragment vẫn được manager quản lý |
| `findFragmentById()` / `findFragmentByTag()` | Tìm trong scope manager đang gọi |
| `addToBackStack()` | Ghi transaction để có thể đảo ngược |
| `popBackStack()` | Yêu cầu đảo ngược entry trên back stack |

`FragmentTransaction.detach()` khác `Fragment.onDetach()`: một cái là thao tác transaction, một cái là lifecycle callback. `hide()` không tương đương `onStop()`; UI bị ẩn vẫn có thể còn ở trạng thái lifecycle cao. [Nguồn: Fragment transactions](https://developer.android.com/guide/fragments/transactions).

### 3.2. commit không chạy ngay

```kotlin
manager.beginTransaction()
    .setReorderingAllowed(true)
    .replace(R.id.container, NextFragment::class.java, null, "next")
    .addToBackStack("open_next")
    .commit()

// Không bảo đảm Fragment "next" đã được thêm ngay tại dòng sau commit().
```

| Cách thực thi | Đặc điểm |
| --- | --- |
| `commit()` | Xếp transaction để thực thi trên main thread |
| `commitNow()` | Thực thi ngay; không dùng cùng addToBackStack |
| `executePendingTransactions()` | Có thể chạy nhiều transaction đang chờ của manager |
| `commitAllowingStateLoss()` | Cho phép thay đổi có nguy cơ không được khôi phục từ saved state |

Dùng `commit()` cho điều hướng thông thường. Không dùng `commitNow()` hoặc `executePendingTransactions()` chỉ để che việc đọc Fragment quá sớm. `setReorderingAllowed(true)` giúp manager xử lý các thay đổi trạng thái hiệu quả và đúng với transition/back stack. [Nguồn: Fragment transactions](https://developer.android.com/guide/fragments/transactions), [FragmentTransaction API](https://developer.android.com/reference/androidx/fragment/app/FragmentTransaction).

### 3.3. Back stack lưu transaction

```text
Ban đầu: Summary
Transaction T: replace Summary bằng Detail, addToBackStack("detail")
Hiện tại: Detail
popBackStack(): đảo ngược T → Summary
```

Back stack không đơn giản là danh sách mọi Fragment từng mở. Một entry có thể gồm nhiều operation, và pop đảo ngược nhóm operation đó. Transaction không thêm vào back stack sẽ không tự được đảo ngược khi Back. `tag` của Fragment, tên back stack entry và ID container là ba khái niệm khác nhau. [Nguồn: FragmentTransaction API](https://developer.android.com/reference/androidx/fragment/app/FragmentTransaction).

### 3.4. Saved state và thời điểm transaction

Không thực hiện transaction thay đổi state sau khi manager đã lưu state để khôi phục; lời gọi thông thường có thể báo lỗi `Can not perform this action after onSaveInstanceState`. Kiểm tra `isStateSaved` là một phần xử lý thời điểm, không phải phép kiểm tra “Fragment đang hiển thị”. [Nguồn: FragmentTransaction API](https://developer.android.com/reference/androidx/fragment/app/FragmentTransaction).

Với yêu cầu đến từ network/timer, lưu ý định điều hướng hoặc mở dialog và xử lý khi host trở lại trạng thái phù hợp, rồi kiểm tra lại manager. Không mặc định dùng `commitAllowingStateLoss()` để làm hết lỗi. Ví dụ phần 8 thông báo thử lại nếu người dùng yêu cầu mở dialog khi state đã được lưu.

## 4. Phân biệt các FragmentManager

**`childFragmentManager` không phải một class khác tên `ChildFragmentManager`.** Nó là thuộc tính trả về một instance có kiểu `FragmentManager`, quản lý các Fragment con của Fragment hiện tại.

### 4.1. Ba cách truy cập thường gặp

| Cách truy cập | Nơi sử dụng | Quản lý ai? |
| --- | --- | --- |
| `supportFragmentManager` | FragmentActivity/AppCompatActivity | Fragment trực tiếp thuộc Activity |
| `parentFragmentManager` | Trong Fragment | Manager đang quản lý chính Fragment này |
| `childFragmentManager` | Trong Fragment | Fragment trực tiếp thuộc Fragment này |

`parentFragmentManager` không có nghĩa là “luôn lấy manager của Activity”. Nếu Fragment hiện tại là child, nó trả manager của Fragment cha đang quản lý child đó. [Nguồn: Fragment manager](https://developer.android.com/guide/fragments/fragmentmanager).

Trong code AndroidX cũ, property `fragmentManager`/`getFragmentManager()` đã deprecated; dùng `parentFragmentManager` để thể hiện rõ đang lấy manager chứa Fragment này. Truy cập parent manager khi Fragment chưa gắn với manager/host có thể ném exception. [Nguồn: Fragment API](https://developer.android.com/reference/androidx/fragment/app/Fragment).

### 4.2. Sơ đồ scope

```text
MainActivity
│
├── M0 = MainActivity.supportFragmentManager
│   └── DashboardFragment
│       │
│       ├── Dashboard.parentFragmentManager == M0
│       │
│       └── M1 = Dashboard.childFragmentManager
│           ├── SummaryFragment hoặc DetailFragment
│           │   ├── parentFragmentManager == M1
│           │   └── childFragmentManager == M2 (scope con riêng nếu dùng)
│           │
│           └── ResetDialogFragment
│               └── parentFragmentManager == M1
```

`M0`, `M1`, `M2` là tên đặt cho bài học. Chúng cùng kiểu nhưng khác instance/scope. Mỗi manager có tập Fragment, back stack và kênh Fragment Result riêng; tìm bằng tag ở M0 không tự tìm xuyên xuống M1. [Nguồn: Fragment API](https://developer.android.com/reference/androidx/fragment/app/Fragment), [Communicate with fragments](https://developer.android.com/guide/fragments/communicate).

### 4.3. Quy tắc chọn manager

| Tình huống | Manager cần chọn |
| --- | --- |
| Activity đặt Dashboard vào activityContainer | Activity.supportFragmentManager |
| Dashboard đặt Summary vào childContainer trong layout của nó | Dashboard.childFragmentManager |
| Summary đổi chính vùng Summary sang Detail | Summary.parentFragmentManager |
| Summary đặt một Fragment vào container riêng bên trong Summary | Summary.childFragmentManager |
| Dashboard mở dialog thuộc Dashboard | Dashboard.childFragmentManager |
| Activity mở dialog thuộc Activity | Activity.supportFragmentManager |

Manager quyết định **quan hệ sở hữu**, không chỉ vị trí View xuất hiện. Dialog vẫn có Window riêng dù được quản lý như child của Dashboard. Muốn dialog có scope Activity thì dùng manager Activity và thiết kế người nhận kết quả theo scope đó.

### 4.4. Ví dụ sai và đúng

```kotlin
// Trong DashboardFragment: childContainer nằm trong layout của Dashboard.

// Sai scope cho mục tiêu tạo một Fragment con của Dashboard:
requireActivity().supportFragmentManager.beginTransaction()
    .replace(R.id.childContainer, SummaryFragment::class.java, null)
    .commit()

// Đúng quan hệ parent/child:
childFragmentManager.beginTransaction()
    .setReorderingAllowed(true)
    .replace(R.id.childContainer, SummaryFragment::class.java, null, "summary")
    .commit()
```

Dùng manager Activity có thể không tìm được container ở thời điểm thực thi, hoặc tạo quan hệ sở hữu/lifecycle không như mong muốn dù giao diện ban đầu trông đúng. Không suy ra manager chỉ từ việc transaction có thể tìm thấy View.

**Bẫy khi dùng Navigation:** Fragment destination thường là child của `NavHostFragment`. Vì vậy `destination.parentFragmentManager` thường là `NavHostFragment.childFragmentManager`, không phải Activity.supportFragmentManager. Với container do Navigation quản lý, dùng NavController để điều hướng; không tự replace container đó để né Navigation. [Nguồn: Fragment manager](https://developer.android.com/guide/fragments/fragmentmanager).

## 5. Nested back stack và nút Back

M0 có thể quản lý điều hướng cấp màn hình, trong khi M1 sở hữu back stack riêng của vùng con. Khi muốn thao tác entry cụ thể của M1, cần xác định đúng manager đó.

Có một điểm cần chú ý: lời gọi `popBackStack()` không chỉ định tên/ID có thể được chuyển xuống child manager của primary navigation Fragment trước. Vì vậy không nên suy luận “gọi ở M0 thì chắc chắn chỉ pop M0”; kết quả còn phụ thuộc primary navigation chain. [Nguồn: FragmentManager API](https://developer.android.com/reference/androidx/fragment/app/FragmentManager?authuser=4).

`setPrimaryNavigationFragment()` chỉ định Fragment có child manager được ưu tiên cho điều hướng Back ở scope đó. Trong bài mẫu, MainActivity đặt Dashboard làm primary navigation Fragment; khi Summary → Detail trên M1, Back có thể trả về Summary trước khi thoát Activity. [Nguồn: Fragment manager](https://developer.android.com/guide/fragments/fragmentmanager).

```text
Activity / M0
└── Dashboard (primary navigation Fragment của M0)
    └── M1 back stack: [open_detail]

Back: M1 đảo ngược open_detail → Summary
Back tiếp theo, không còn entry: trở về xử lý của host
```

Không phải mọi child manager tự động nhận Back chỉ vì có back stack. Primary navigation chain và cơ chế Back của host cần đúng. Khi dùng Navigation, để NavHost/NavController quản lý; nếu tự xử lý Back, dùng cơ chế hiện đại như OnBackPressedDispatcher và kiểm tra thứ tự các scope.

## 6. DialogFragment

DialogFragment là Fragment chuyên tạo/quản lý dialog. So với tự giữ một AlertDialog trong Activity/Fragment, nó cho phép FragmentManager quản lý lifecycle và restore dialog khi cấu hình thay đổi. [Nguồn: Display dialogs with DialogFragment](https://developer.android.com/guide/fragments/dialogs).

### 6.1. Hai cách tạo UI

| Cách | Khi dùng | LifecycleOwner cho observer |
| --- | --- | --- |
| Override `onCreateDialog()` | AlertDialog với title/message/buttons | DialogFragment hoặc owner phù hợp; không giả định có viewLifecycleOwner |
| Trả View từ `onCreateView()` / constructor layout | Dialog có layout riêng | viewLifecycleOwner khi Fragment thực sự tạo View |

Đặt một View bằng `AlertDialog.Builder.setView()` không tự biến View đó thành `Fragment.view`. Với dialog chỉ tạo từ `onCreateDialog()`, `onViewCreated()` không được gọi nếu Fragment không trả View từ `onCreateView()`. [Nguồn: Display dialogs with DialogFragment](https://developer.android.com/guide/fragments/dialogs).

### 6.2. Show, dismiss và cancel

| API / callback | Ý nghĩa |
| --- | --- |
| `show(manager, tag)` | Thêm dialog bằng transaction bất đồng bộ |
| `showNow(manager, tag)` | Thêm ngay bằng commitNow; không thêm back stack |
| `dismiss()` | Đóng dialog và cập nhật FragmentManager |
| `onCancel()` | Nhận khi dialog bị cancel, ví dụ Back/tap ngoài nếu cho phép |
| `onDismiss()` | Nhận khi dialog bị dismiss; không chứng minh người dùng đã xác nhận |
| `isCancelable` | Điều khiển khả năng cancel ở DialogFragment |

Không tự gán `Dialog.setOnCancelListener()`/`setOnDismissListener()` vì DialogFragment quản lý các listener đó. Override callback tương ứng và gọi `super`. Không đặt logic “xác nhận thành công” trong `onDismiss()`: dismiss còn xảy ra sau hủy hoặc trong quá trình tháo UI. [Nguồn: DialogFragment API](https://developer.android.com/reference/androidx/fragment/app/DialogFragment).

### 6.3. Scope và tránh dialog trùng

Dialog thuộc Fragment được mở bằng `childFragmentManager`. Dialog được restore thì không tạo thêm instance sau mỗi `onViewCreated()`; dùng tag để tìm và chỉ mở theo hành động phù hợp. [Nguồn: Display dialogs with DialogFragment](https://developer.android.com/guide/fragments/dialogs).

Chỉ kiểm tra `findFragmentByTag()` rồi gọi `show()` chưa chặn được hai lời gọi liên tiếp trước khi transaction chạy. Bài mẫu dùng `showNow()` trong click listener trên main thread để kiểm tra tag có hiệu lực ngay. Không dùng nó từ callback đang thực thi transaction khác; với `show()` có thể dùng cờ đang chờ hoặc chống click lặp.

## 7. Fragment Result: manager phải khớp

Để trả kết quả một lần, dùng Fragment Result API. Người gửi và người nhận phải dùng **cùng instance FragmentManager** và cùng `requestKey`.

```text
Dashboard mở ResetDialog trên M1 = Dashboard.childFragmentManager

Dashboard nhận: M1.setFragmentResultListener(...)
Dialog gửi:    parentFragmentManager.setFragmentResult(...)
               ↑ manager này cũng là M1
```

Listener nhận khi LifecycleOwner ở trạng thái ít nhất STARTED. Với mỗi key chỉ có một listener và một pending result; nếu chưa thể giao, result mới thay pending result cũ. Sau khi giao, result được xóa. Đây không phải hàng đợi mọi sự kiện hoặc nơi lưu dữ liệu nghiệp vụ dài hạn. [Nguồn: Communicate with fragments](https://developer.android.com/guide/fragments/communicate).

| Quan hệ | Manager đăng ký listener | Manager gửi result |
| --- | --- | --- |
| Hai Fragment cùng manager | parentFragmentManager của bên nhận | parentFragmentManager của bên gửi |
| Parent nhận từ child/dialog child | childFragmentManager của parent | parentFragmentManager của child |
| Activity nhận từ Fragment trực tiếp | supportFragmentManager của Activity | parentFragmentManager của Fragment |

Nếu listener cập nhật UI của Fragment, đăng ký trong `onViewCreated()` với `viewLifecycleOwner` cho mỗi lần tạo View. Kết quả quan trọng nên chuyển vào state của màn hình để UI render lại được. Shared ViewModel phù hợp hơn khi nhiều Fragment cần dùng chung state liên tục; scope ViewModel phải được chọn rõ ràng.

## 8. Ví dụ xuyên suốt: Dashboard và dialog thuộc Dashboard

### 8.1. Cây file

```text
java/com/example/fragmentlesson/
├── MainActivity.kt
├── DashboardFragment.kt
├── SummaryFragment.kt
├── DetailFragment.kt
└── ResetDialogFragment.kt

res/layout/
├── activity_main.xml
├── fragment_dashboard.xml
├── fragment_summary.xml
└── fragment_detail.xml
res/values/strings.xml
```

Manifest cần khai báo MainActivity; theme Activity phải tương thích AppCompat. Ví dụ dùng API transaction trực tiếp, không cần extension `commit {}` để intern thấy rõ các bước.

### 8.2. Các layout

File `res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/activityContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

File `res/layout/fragment_dashboard.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    <TextView
        android:id="@+id/statusText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/reset_not_confirmed" />
    <Button
        android:id="@+id/openDialogButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/open_reset_dialog" />
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/childContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
</LinearLayout>
```

File `res/layout/fragment_summary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/summary_title" />
    <Button
        android:id="@+id/openDetailButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/open_detail" />
</LinearLayout>
```

File `res/layout/fragment_detail.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/detail_title" />
    <Button
        android:id="@+id/backButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/back_to_summary" />
</LinearLayout>
```

File `res/values/strings.xml`; nếu file đã tồn tại thì thêm các item vào `<resources>` đang có:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Fragment Lesson</string>
    <string name="summary_title">Fragment tổng quan</string>
    <string name="detail_title">Fragment chi tiết</string>
    <string name="open_detail">Mở chi tiết</string>
    <string name="back_to_summary">Quay lại tổng quan</string>
    <string name="open_reset_dialog">Mở dialog đặt lại</string>
    <string name="reset_title">Đặt lại bộ lọc?</string>
    <string name="reset_message">Bài mẫu chỉ ghi nhận xác nhận, chưa thay đổi dữ liệu.</string>
    <string name="confirm">Xác nhận</string>
    <string name="cancel">Hủy</string>
    <string name="reset_confirmed">Đã xác nhận đặt lại bộ lọc</string>
    <string name="reset_not_confirmed">Chưa có xác nhận</string>
    <string name="try_again">Màn hình chưa sẵn sàng. Hãy thử lại.</string>
</resources>
```

`FragmentContainerView` là container dành cho Fragment; không dùng thẻ `<fragment>` cũ cho bài này. [Nguồn: Create a fragment](https://developer.android.com/guide/fragments/create).

### 8.3. MainActivity: dùng M0

File `MainActivity.kt`:

```kotlin
package com.example.fragmentlesson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val dashboard = DashboardFragment()
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.activityContainer, dashboard, "dashboard")
                .setPrimaryNavigationFragment(dashboard)
                .commit()
        }
    }
}
```

Chỉ thêm Dashboard lần đầu; lần tạo lại để FragmentManager restore. Instance được tạo trực tiếp ở đây để đặt primary navigation trong cùng transaction; class có constructor không tham số nên factory mặc định tạo lại được. Các transaction phía dưới dùng overload nhận Class.

### 8.4. DashboardFragment: dùng M1 cho UI con và dialog

File `DashboardFragment.kt`:

```kotlin
package com.example.fragmentlesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var resetConfirmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetConfirmed = savedInstanceState?.getBoolean(STATE_CONFIRMED) ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val status = view.findViewById<TextView>(R.id.statusText)
        fun renderStatus() {
            status.setText(
                if (resetConfirmed) R.string.reset_confirmed
                else R.string.reset_not_confirmed
            )
        }
        renderStatus()

        // Dialog gửi trên parentFragmentManager của nó: cùng M1 này.
        childFragmentManager.setFragmentResultListener(
            ResetDialogFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, result ->
            if (result.getBoolean(ResetDialogFragment.KEY_CONFIRMED)) {
                resetConfirmed = true
                renderStatus()
            }
        }

        // Fragment con đã restore thì không thêm Summary lần nữa.
        if (childFragmentManager.findFragmentById(R.id.childContainer) == null) {
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.childContainer, SummaryFragment::class.java, null, "summary"
                )
                .commit()
        }

        view.findViewById<Button>(R.id.openDialogButton).setOnClickListener {
            val manager = childFragmentManager
            if (manager.isStateSaved) {
                Toast.makeText(requireContext(), R.string.try_again, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (manager.findFragmentByTag(ResetDialogFragment.TAG) == null) {
                // Click trên main thread, ngoài quá trình thực thi transaction khác.
                ResetDialogFragment().showNow(manager, ResetDialogFragment.TAG)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_CONFIRMED, resetConfirmed)
    }

    companion object {
        private const val STATE_CONFIRMED = "state_reset_confirmed"
    }
}
```

Listener gắn với View lifecycle, không giữ TextView của lần tạo UI cũ. Boolean nhỏ được lưu để render lại trạng thái sau recreation; dữ liệu bộ lọc thật nên do tầng state/nghiệp vụ quản lý.

### 8.5. Summary và Detail: parentFragmentManager của child là M1

File `SummaryFragment.kt`:

```kotlin
package com.example.fragmentlesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class SummaryFragment : Fragment(R.layout.fragment_summary) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val openButton = view.findViewById<Button>(R.id.openDetailButton)
        openButton.isEnabled = true
        openButton.setOnClickListener {
            val manager = parentFragmentManager // Dashboard.childFragmentManager
            if (manager.isStateSaved) return@setOnClickListener
            openButton.isEnabled = false // Chặn enqueue nhiều transaction do click lặp.
            manager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.childContainer, DetailFragment::class.java, null, "detail")
                .addToBackStack("open_detail")
                .commit()
        }
    }
}
```

File `DetailFragment.kt`:

```kotlin
package com.example.fragmentlesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class DetailFragment : Fragment(R.layout.fragment_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backButton = view.findViewById<Button>(R.id.backButton)
        backButton.isEnabled = true
        backButton.setOnClickListener {
            if (parentFragmentManager.isStateSaved) return@setOnClickListener
            backButton.isEnabled = false
            parentFragmentManager.popBackStack()
        }
    }
}
```

Summary và Detail là sibling thuộc Dashboard. Vì thế transaction thay chính vùng của chúng đi qua `parentFragmentManager`; dùng `childFragmentManager` ở Summary sẽ nhắm scope bên trong Summary, khác với mục tiêu thay Summary bằng Detail.

### 8.6. ResetDialogFragment: trả kết quả qua M1

File `ResetDialogFragment.kt`:

```kotlin
package com.example.fragmentlesson

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class ResetDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.reset_title)
            .setMessage(R.string.reset_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val result = Bundle().apply { putBoolean(KEY_CONFIRMED, true) }
                parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        const val TAG = "reset_filter_dialog"
        const val REQUEST_KEY = "reset_filter_request"
        const val KEY_CONFIRMED = "confirmed"
    }
}
```

Positive button của AlertDialog tự đóng dialog theo hành vi mặc định. Chỉ nhánh xác nhận gửi result; Hủy, Back hoặc tap ngoài không gửi xác nhận. Trong class này không dùng `viewLifecycleOwner` vì UI do Dialog tạo, không có View trả từ `onCreateView()`.

### 8.7. Những điều cần quan sát khi chạy

1. Dashboard nằm trong M0; Summary/Detail/dialog nằm trong M1.
2. Summary → Detail làm tăng backStackEntryCount của M1, không tăng M0.
3. Nút Back ở Detail pop M1; system Back cũng cần hoạt động đúng nhờ primary navigation.
4. Dialog thuộc Dashboard, nên listener ở Dashboard dùng childFragmentManager.
5. Xoay khi ở Detail: không tự quay về Summary do thêm lại Fragment ban đầu.
6. Xoay khi dialog mở: manager restore dialog; không tạo một dialog mới vô điều kiện.
7. Xác nhận rồi xoay: trạng thái text được render lại từ saved state.

## 9. Debug scope của manager

Đặt snippet trong `DashboardFragment.onViewCreated()` để quan sát; import `android.util.Log`:

```kotlin
Log.d("FragmentLesson", "parentManager=${parentFragmentManager}")
Log.d("FragmentLesson", "childManager=${childFragmentManager}")
Log.d("FragmentLesson", "childFragments=${childFragmentManager.fragments}")
Log.d("FragmentLesson", "childBackStack=${childFragmentManager.backStackEntryCount}")

val dialogAtParent = parentFragmentManager.findFragmentByTag(ResetDialogFragment.TAG)
val dialogAtChild = childFragmentManager.findFragmentByTag(ResetDialogFragment.TAG)
```

Khi dialog thuộc M1 đã được thêm, tìm ở M0 thường trả null, tìm ở M1 thấy dialog. Không kết luận ngay sau `commit()` rằng transaction thất bại chỉ vì chưa tìm thấy Fragment. Có thể quan sát trong callback lifecycle/back stack thích hợp; `FragmentLifecycleCallbacks` hữu ích khi debug cây Fragment.

### Các lỗi thường gặp

| Triệu chứng | Điều cần kiểm tra |
| --- | --- |
| `No view found for id` | Container tồn tại chưa? ID đúng không? Manager có đúng scope host không? |
| Back không quay về màn hình trước | Transaction có addToBackStack? Pop đúng manager? Primary navigation đúng chưa? |
| Dialog/Fragment trùng sau xoay | Có thêm lại mặc dù manager đã restore không? |
| Result không đến | Manager có cùng instance? requestKey khớp? Owner đã STARTED chưa? |
| Lỗi sau onSaveInstanceState | Transaction được gọi quá muộn? Có ý định cần trì hoãn không? |
| Lỗi viewLifecycleOwner trong dialog | Dialog chỉ dùng onCreateDialog hay thực sự trả Fragment View? |
| Dùng constructor có tham số bị lỗi restore | Có arguments/FragmentFactory đúng cách chưa? |
| UI cập nhật vào View cũ | Observer/listener có gắn với View lifecycle không? Binding có được xóa không? |
| Ẩn Fragment nhưng tác vụ vẫn chạy | hide không tự giảm lifecycle; tác vụ đang theo owner nào? |

## 10. Bài tập thực hành

### Bài A — Giải thích manager bằng sơ đồ

Vẽ Activity → ParentFragment → ChildFragment → DialogFragment. Chỉ rõ dialog được mở ở scope nào. Với mỗi node, ghi parentFragmentManager trỏ tới manager nào và childFragmentManager quản lý ai.

**Tiêu chí đạt:** không nói parentFragmentManager luôn là manager Activity; không coi childFragmentManager là một class khác FragmentManager.

### Bài B — Transaction và Back

Tạo Summary → Detail; thử có và không có `addToBackStack()`. Log lifecycle, backStackEntryCount và giải thích Fragment View có thể bị hủy trước khi Fragment bị hủy hoàn toàn.

**Tiêu chí đạt:** Back đảo ngược đúng transaction; phân biệt tag Fragment với tên entry.

### Bài C — Nested Fragment

Dashboard có hai container con: danh sách và preview. Dùng childFragmentManager để thêm Fragment cho mỗi vùng. Thử tìm các child ở manager Activity rồi giải thích kết quả.

**Tiêu chí đạt:** ownership đúng; không dùng manager Activity để tạo child chỉ vì tìm được container.

### Bài D — Dialog và kết quả

Nâng ResetDialog thành dialog chọn bộ lọc. Truyền lựa chọn ban đầu bằng arguments, trả lựa chọn bằng Fragment Result. Giữ state khi xoay; hủy không áp dụng lựa chọn mới.

**Tiêu chí đạt:** hai bên dùng cùng manager/key; không giữ callback lambda tùy ý trong field của dialog để thay cho cơ chế restore.

### Bài tổng hợp — Dashboard quản lý màn hình con

Yêu cầu nghiệm thu:

- Activity chứa Dashboard; Dashboard quản lý Summary/Detail và dialog.
- Thao tác Summary → Detail có back stack ở scope child.
- Back xử lý dialog trước khi trở về UI bên dưới; sau đó Detail → Summary.
- Dialog không tạo trùng khi click nhanh hoặc khi xoay màn hình.
- Dialog trả dữ liệu qua Fragment Result trên đúng manager.
- Fragment có View dùng View lifecycle cho các observer cập nhật UI.
- Arguments và state được khôi phục; không giữ instance Fragment cũ sau recreation.
- Giải thích được tác hại của giao dịch sau saved state và cách trì hoãn yêu cầu.

## 11. Ma trận kiểm tra và đánh giá

| Tình huống | Kết quả mong đợi |
| --- | --- |
| Mở app lần đầu | Một Dashboard và một Summary |
| Mở Detail rồi Back | Trở lại Summary; đúng child back stack |
| Click mở Detail liên tiếp | Không enqueue nhiều entry ngoài ý muốn |
| Mở dialog rồi Hủy | Không cập nhật trạng thái xác nhận |
| Mở dialog rồi xác nhận | Dashboard nhận đúng một result của lần xác nhận |
| Click mở dialog liên tiếp | Một dialog theo tag |
| Back khi dialog mở | Đóng dialog trước khi điều hướng màn hình bên dưới |
| Xoay ở Detail | Detail và child back stack được khôi phục |
| Xoay khi dialog mở | Dialog được restore, không nhân đôi |
| Xoay sau xác nhận | Text/state vẫn đúng |
| Đưa app background rồi có yêu cầu mở dialog | Trì hoãn hoặc báo thử lại; không transaction sau saved state |
| Listener chưa STARTED | Pending result chờ; không cập nhật View đã hủy |
| Gửi result trên sai manager | Không nhận; giải thích được nguyên nhân |
| Process recreation có saved state | Khôi phục arguments và state đã lưu; không dựa vào biến RAM cũ |

Xoay màn hình kiểm tra configuration recreation, **không thay thế hoàn toàn kiểm tra process death**. Tùy chọn “Don't keep activities” cũng không đồng nghĩa kill process. Kiểm tra process recreation bằng quy trình kiểm thử phù hợp, tránh force-stop như một phép chứng minh saved state sẽ được phục hồi. [Nguồn: Saving state with fragments](https://developer.android.com/guide/fragments/saving-state).

| Hạng mục | Điểm |
| --- | ---: |
| Lifecycle và xử lý View | 15 |
| Transaction, commit và back stack | 20 |
| Chọn đúng scope FragmentManager | 30 |
| DialogFragment và tránh tạo trùng | 15 |
| Fragment Result, arguments và restore | 20 |
| **Tổng** | **100** |

Mức đạt đề xuất: từ 75 điểm. Cần sửa lỗi sai scope manager, crash khi restore và nhân đôi dialog trước khi nghiệm thu.

## 12. Câu hỏi ôn tập và đáp án ngắn

1. **FragmentManager và childFragmentManager có phải hai class khác nhau?** Không; childFragmentManager là property trả một FragmentManager cho scope con.
2. **parentFragmentManager quản lý ai?** Chính Fragment đang gọi và các sibling thuộc cùng manager.
3. **ParentFragment là Fragment trực tiếp của Activity thì manager nào quản lý nó?** Activity.supportFragmentManager.
4. **ChildFragment.parentFragmentManager là gì?** ParentFragment.childFragmentManager.
5. **Parent muốn thêm Fragment vào layout riêng dùng manager nào?** Parent.childFragmentManager.
6. **Child muốn thay chính nó bằng sibling trong cùng container?** Child.parentFragmentManager.
7. **Tại sao không find được child bằng manager Activity?** Đang tìm sai scope; không tự tìm xuyên cây.
8. **commit có chạy ngay không?** Không; thường xếp để chạy trên main thread.
9. **commitNow có dùng với addToBackStack không?** Không.
10. **Back stack lưu mọi Fragment đã mở?** Không; lưu các transaction được thêm vào back stack.
11. **hide có gọi onStop không?** Không tự đổi lifecycle.
12. **Dialog thuộc ParentFragment mở bằng gì?** Parent.childFragmentManager.
13. **Dialog này gửi kết quả bằng manager nào?** Dialog.parentFragmentManager; cũng là manager mà Parent dùng để nhận.
14. **onDismiss có nghĩa là xác nhận?** Không; xác nhận phải được nhận biết từ hành động cụ thể.
15. **Dialog chỉ dùng onCreateDialog có viewLifecycleOwner không?** Không được giả định có; nó không tạo Fragment View chỉ vì Dialog có UI.
16. **Xoay màn hình có cần tự thêm lại Fragment/dialog luôn không?** Không; manager có thể restore chúng.
17. **Listener chưa STARTED thì result thế nào?** Có thể được giữ chờ; cùng key chỉ giữ pending result mới nhất.
18. **Destination trong NavHost có parent manager là manager Activity không?** Thường không; thường là child manager của NavHost.

## 13. Checklist tự đánh giá

- [ ] Tôi phân biệt Fragment lifecycle và View lifecycle.
- [ ] Tôi dùng AndroidX Fragment và container phù hợp.
- [ ] Tôi hiểu add/replace/remove/show/hide/attach/detach.
- [ ] Tôi giải thích được commit bất đồng bộ và giới hạn của commitNow.
- [ ] Tôi hiểu back stack lưu transaction và pop đúng scope.
- [ ] Tôi phân biệt tag, container ID và tên back stack entry.
- [ ] Tôi vẽ được support/parent/child FragmentManager trong cây Fragment.
- [ ] Tôi không coi parentFragmentManager luôn là manager Activity.
- [ ] Tôi tạo nested Fragment bằng manager của Fragment cha.
- [ ] Tôi mở DialogFragment trên scope có chủ đích và chống tạo trùng.
- [ ] Tôi phân biệt xác nhận, cancel và dismiss.
- [ ] Tôi gửi/nhận Fragment Result bằng cùng manager và requestKey.
- [ ] Tôi xử lý View lifecycle, arguments và saved state khi recreation.
- [ ] Tôi xử lý yêu cầu transaction đến sau saved state.

## 14. Hướng dẫn đọc nguồn

Nguồn được gắn tại từng phần là **Android Developers của Google**. Lộ trình, giải thích tiếng Việt, ví dụ và bài tập được biên soạn cho intern; không phải bản dịch nguyên văn.

Khi đọc code dự án, xác định theo thứ tự: **Fragment nằm ở đâu trong cây → ai quản lý nó → transaction/result đi qua manager nào → lifecycle/state hiện tại có cho phép thao tác không**. Với dự án dùng Navigation, học FragmentManager để hiểu cơ chế, rồi dùng API điều hướng mà dự án đã chọn cho container của NavHost.
