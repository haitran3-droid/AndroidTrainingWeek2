# Bài giảng Android: Mô hình MVVM — Level Intern

**Đối tượng:** intern đã biết Kotlin, Activity, Fragment, View và XML.  
**Phạm vi:** MVVM trong Android dùng Views, AndroidX ViewModel, coroutines, LiveData và Flow.  
**Thời lượng gợi ý:** 4 buổi × 3 giờ, cộng thời gian làm bài tập.  
**Ngày đối chiếu nguồn:** 06/10/2026.  
**Bài trước:** [Fragment và DialogFragment](BAI_GIANG_FRAGMENT_DIALOGFRAGMENT_INTERN.md).

Ví dụ có tên file được viết để đưa vào một ứng dụng Android dùng Views. Thay package `com.example.mvvmlesson` bằng package thực tế; import `R` từ namespace của ứng dụng nếu cần. Workspace hiện chỉ có tài liệu, chưa có ứng dụng Android/Gradle để biên dịch và chạy các ví dụ.

Các thư viện cần cho bài thực hành: AndroidX Fragment KTX, AppCompat, Core KTX, Lifecycle ViewModel/Runtime KTX và ViewModel SavedState, Kotlin coroutines Android; thêm Lifecycle LiveData KTX khi làm phần LiveData. Chọn phiên bản tương thích theo cấu hình dự án. Ví dụ dùng `viewModelFactory`, `createSavedStateHandle` và `SavedStateHandle.getStateFlow`, có từ Lifecycle 2.5.0; không copy nguyên ví dụ vào dự án dùng Lifecycle cũ hơn mà chưa kiểm tra API.

**Trạng thái kiểm chứng:** đã kiểm tra cấu trúc Markdown, cú pháp các khối XML và tài nguyên được tham chiếu trong ví dụ chính. Kotlin chưa được build hoặc chạy; dùng ma trận kiểm tra ở phần 12 khi triển khai.

## 1. Mục tiêu và lộ trình

| Nội dung | Sau bài học, intern làm được |
| --- | --- |
| MVVM | Tách View, ViewModel và Model; xây màn hình theo UI state |
| ViewModel lifecycle | Chọn đúng owner, giải thích sống qua xoay và thời điểm clear |
| viewModelScope | Chạy tác vụ bất đồng bộ, xử lý cancellation và lỗi đúng |
| LiveData | Đóng gói mutable data; observe theo View lifecycle; dùng transformation |
| Flow | Phân biệt cold/hot; dùng StateFlow, operators và collection theo lifecycle |
| Thực hành | Màn hình tải dữ liệu, tìm kiếm, retry, loading/error/empty và restore |

| Buổi | Nội dung | Thực hành |
| --- | --- | --- |
| 1 | MVVM, UI state và owner của ViewModel | Tách một Fragment đang làm quá nhiều việc |
| 2 | Coroutines, viewModelScope và LiveData | Counter; tải dữ liệu; xử lý lỗi và hủy |
| 3 | Flow, StateFlow và lifecycle collection | Lọc danh sách; combine; dừng/khởi động lại collector |
| 4 | Saved state, tích hợp và kiểm thử | Xoay màn hình, retry, so sánh LiveData/Flow |

## 2. MVVM là gì?

MVVM viết tắt của **Model–View–ViewModel**. Trong bài này, áp dụng mô hình cùng luồng dữ liệu một chiều: View gửi action; ViewModel xử lý và cung cấp state; View render state. Kiến trúc Android còn tổ chức UI layer, data layer và domain layer tùy nhu cầu; không bắt buộc mọi lớp đều tương ứng một chữ trong MVVM. [Nguồn: Architecture recommendations for Views](https://developer.android.com/topic/architecture/views/recommendations-views).

```text
Người dùng
    │ click / nhập từ khóa
    ▼
View: Activity / Fragment / các View
    │ action: refresh(), setQuery(...)
    ▼
ViewModel: logic màn hình + UI state
    │ gọi hàm suspend / quan sát Flow
    ▼
Model: Repository → data source (API / database / bộ nhớ)
    │ dữ liệu
    ▼
ViewModel tạo UI state → View quan sát và render
```

### 2.1. Trách nhiệm của từng phần

| Thành phần | Nên làm | Ví dụ |
| --- | --- | --- |
| View | Hiển thị, nhận tương tác, thao tác UI | Gắn click; hiển thị ProgressBar; điều hướng |
| ViewModel | Giữ state và điều phối logic màn hình | Lọc danh sách; gọi refresh; chuyển lỗi thành state |
| Model/data layer | Quản lý dữ liệu và quy tắc liên quan | Repository lấy API, đọc DB, cập nhật cache |
| Use case, nếu cần | Logic được dùng lại hoặc đủ phức tạp | Kết hợp nhiều repository theo quy tắc nghiệp vụ |

Model không chỉ là một `data class`. Repository là điểm truy cập dữ liệu; data source làm việc với nguồn cụ thể. ViewModel không nên gọi Retrofit/DAO trực tiếp nếu đã có repository chịu trách nhiệm đó. Domain layer không cần thêm chỉ để đủ số lớp trong một bài nhỏ. [Nguồn: Data layer](https://developer.android.com/topic/architecture/data-layer).

### 2.2. Các nguyên tắc triển khai

- View không tự sửa mutable state trong ViewModel; gọi các hàm thể hiện action.
- ViewModel không giữ Activity, Fragment, View, Binding hoặc callback chứa các đối tượng đó.
- Repository không tham chiếu UI; hàm suspend của data layer phải an toàn khi gọi từ main thread.
- State đủ để render lại màn hình; tránh nhiều boolean rời rạc tạo tổ hợp vô nghĩa.
- Dependency truyền qua constructor/factory để thay bằng fake khi kiểm thử.

MVVM không bắt buộc dùng Data Binding XML hoặc Hilt. Bài này dùng `findViewById` và factory thủ công để tập trung vào trách nhiệm và flow dữ liệu. [Nguồn: Architecture recommendations for Views](https://developer.android.com/topic/architecture/views/recommendations-views).

### 2.3. UI state và action

```kotlin
data class ListUiState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val error: String? = null
)
```

Đây là ví dụ khái niệm. Với ứng dụng thật, có thể dùng mã lỗi/domain error rồi để UI ánh xạ sang resource text. `data class` phù hợp nếu muốn giữ dữ liệu cũ khi refresh; `sealed interface` phù hợp khi các trạng thái loại trừ nhau.

Action như `refresh()` là ý định của người dùng; state như `isLoading = true` là kết quả mà UI cần render. Không đặt mọi logic nghiệp vụ vào View chỉ vì View đang chứa button.

## 3. Vòng đời ViewModel

### 3.1. ViewModel sống theo ViewModelStoreOwner

ViewModel được tạo khi được yêu cầu từ một owner và được giữ trong ViewModelStore. Nó còn tồn tại qua configuration change; được clear khi owner kết thúc vĩnh viễn theo scope của owner. [Nguồn: ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel).

```text
Activity A / Fragment F / View cũ
                 │ xoay màn hình
                 ▼
Activity A' / Fragment F' / View mới
                 │
                 └── ViewModel vẫn là instance đang được owner giữ

Owner bị gỡ vĩnh viễn → ViewModelStore clear → scope bị hủy, onCleared()
```

Không phải mọi `onDestroy()` của Activity đều làm clear ViewModel: configuration change có cơ chế giữ lại store. `onDestroyView()` của Fragment cũng không đồng nghĩa ViewModel của Fragment bị clear.

### 3.2. Chọn scope trong Fragment

| Cách lấy | Owner | Dùng khi |
| --- | --- | --- |
| `by viewModels()` | Fragment hiện tại | State của màn hình này |
| `by activityViewModels()` | Activity chứa Fragment | State thật sự dùng chung giữa các Fragment của Activity |
| `by viewModels({ requireParentFragment() })` | Fragment cha | Các child dùng chung state của parent |
| `by navGraphViewModels(R.id.graph)` | Back stack entry của graph | State dùng trong một luồng Navigation |

Các delegate trong bảng cần import KTX phù hợp; `navGraphViewModels` cần Navigation Fragment KTX. Hai Fragment có `by viewModels()` cùng class vẫn có hai instance khác nhau nếu owner khác nhau. Cùng owner và cùng key lấy lại cùng instance; class/key khác có thể có instance khác. [Nguồn: ViewModel Scoping APIs](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis).

Liên hệ bài Fragment: hai Fragment cùng `parentFragmentManager` chưa chắc dùng chung ViewModel. FragmentManager quản lý cây Fragment; ViewModelStoreOwner quyết định scope lưu ViewModel. `R.id.graph` trong bảng là ID graph minh họa, cần được khai báo trong dự án dùng Navigation.

```kotlin
// Trong Fragment:
private val screenViewModel: ScreenViewModel by viewModels()
private val sharedViewModel: SharedViewModel by activityViewModels()
```

Không dùng `ScreenViewModel()` trực tiếp làm cách lấy ViewModel trong UI: instance đó không tự được store của Fragment quản lý. Dependency có constructor riêng được tạo qua factory, nhưng ViewModel vẫn phải lấy qua provider/delegate.

### 3.3. Các tình huống cần giải thích

| Tình huống | ViewModel scoped Fragment/Activity |
| --- | --- |
| Xoay màn hình | Thường giữ lại instance theo owner tương ứng |
| App chuyển background | Không tự clear chỉ vì STOPPED |
| Fragment View bị hủy nhưng Fragment còn được giữ trong back stack | Fragment ViewModel còn có thể tồn tại |
| Fragment bị remove vĩnh viễn | Fragment ViewModel được clear |
| Activity finish | ViewModel scoped Activity được clear |
| Process bị hệ thống kill | Instance trong RAM mất; không bảo đảm onCleared được gọi |

ViewModel không phải bộ nhớ bền vững. `onCleared()` dùng cleanup tài nguyên theo scope, không phải nơi bảo đảm lưu dữ liệu trước mọi kiểu process death. [Nguồn: ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel), [ViewModel API](https://developer.android.com/reference/androidx/lifecycle/ViewModel).

### 3.4. SavedStateHandle khác database

SavedStateHandle lưu state nhỏ như từ khóa, ID đang chọn hoặc bộ lọc để khôi phục từ saved state khi hệ thống tạo lại owner. Instance ViewModel cũ không sống qua process death; ViewModel mới nhận dữ liệu đã lưu. Saved state gắn với task, không phải lời hứa giữ dữ liệu sau force-stop hoặc xóa task. [Nguồn: Saved State module](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate).

Database/DataStore dùng khi cần lưu bền vững. Không nhét danh sách lớn, bitmap hoặc repository vào SavedStateHandle. Ví dụ phần 9 lưu từ khóa, còn danh sách được tải lại.

## 4. viewModelScope

### 4.1. Scope không phải thread

`viewModelScope` là CoroutineScope gắn với ViewModel. Mặc định trên Android dùng `SupervisorJob` và `Dispatchers.Main.immediate`; tự hủy khi ViewModel được clear. Có thể cấu hình scope khác qua API phù hợp, nhưng bài này dùng mặc định. [Nguồn: ViewModelKt API](https://developer.android.com/reference/androidx/lifecycle/ViewModelKt).

```kotlin
fun refresh() {
    viewModelScope.launch {
        val items = repository.fetchItems() // suspend và main-safe
        // Cập nhật state.
    }
}
```

`launch` không tự chuyển mọi việc sang background. `suspend` không có nghĩa là chạy IO, cũng không làm một hàm blocking hết blocking. Chuyển dispatcher tại nơi thực hiện công việc blocking/CPU nặng, thường ở data layer:

```kotlin
// Trong repository; ioDispatcher được inject khi cần kiểm thử.
suspend fun readBlockingFile(): String = withContext(ioDispatcher) {
    file.readText()
}
```

Snippet cần `file` và dispatcher được cung cấp. Các thư viện có API suspend main-safe như nhiều API network/Room thường đã quản lý việc thực thi; không bọc mọi hàm suspend trong IO một cách máy móc. [Nguồn: Coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

### 4.2. Hủy tác vụ và xử lý lỗi

```kotlin
viewModelScope.launch {
    try {
        repository.refresh()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: IOException) {
        // Chuyển lỗi dự kiến thành UI state.
    } finally {
        // Cleanup đồng bộ / cập nhật trạng thái cần thiết.
    }
}
```

Coroutine cancellation có tính hợp tác: `delay` và nhiều hàm suspend nhận hủy; vòng lặp tính toán dài cần kiểm tra cancellation. Không nuốt `CancellationException` trong `catch(Exception)` hoặc `runCatching` rồi biến thành lỗi network. Với lỗi dự kiến, xử lý cụ thể; lỗi lập trình không nên bị che bằng một thông báo chung. [Nguồn: Coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

SupervisorJob cho phép các job con trực tiếp thất bại độc lập. Nó **không tự xử lý exception để tránh crash ứng dụng**; lỗi chưa được xử lý từ `launch` vẫn có thể đi tới uncaught exception handler. Quan hệ job lồng nhau cũng cần được xét riêng.

### 4.3. Chọn scope theo công việc

| Scope/cơ chế | Kết thúc theo | Công việc phù hợp |
| --- | --- | --- |
| viewModelScope | ViewModel clear | Tải/cập nhật dữ liệu của màn hình |
| viewLifecycleOwner.lifecycleScope | Fragment View bị hủy | Coroutine phục vụ UI của lần tạo View này |
| repeatOnLifecycle(STARTED) | Block được hủy dưới STARTED, chạy lại khi STARTED | Collect dữ liệu để render UI |
| Scope dài hơn được quản lý rõ / WorkManager | Theo thiết kế tác vụ | Công việc cần vượt lifecycle màn hình hoặc có yêu cầu chạy bền vững |

Background không tự hủy job trong viewModelScope. Dừng UI collector cũng không tự hủy mọi job ViewModel. Muốn upload/download tiếp tục sau khi rời màn hình phải thiết kế ownership phù hợp; không đổi sang GlobalScope để né hủy. [Nguồn: Lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines), [Coroutines best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

## 5. LiveData cho observable data

### 5.1. Cơ chế quan sát

LiveData là observable holder nhận biết LifecycleOwner. Observer có lifecycle thường hoạt động khi owner STARTED hoặc RESUMED; khi owner bị destroy thì observer được gỡ. Khi hoạt động lại, UI nhận giá trị mới nhất phù hợp, không phải mọi thay đổi đã xảy ra lúc inactive. [Nguồn: LiveData overview](https://developer.android.com/topic/libraries/architecture/livedata).

```kotlin
// Trong ViewModel:
private val _count = MutableLiveData(0)
val count: LiveData<Int> = _count

fun increment() {
    _count.value = (_count.value ?: 0) + 1 // Gọi trên main thread.
}

// Trong Fragment.onViewCreated(), với viewModel và countText đã có:
viewModel.count.observe(viewLifecycleOwner) { count ->
    countText.text = count.toString()
}
```

View chỉ thấy LiveData; quyền cập nhật nằm trong ViewModel. Với UI của Fragment, dùng `viewLifecycleOwner`, không dùng Fragment làm owner chỉ vì tiện viết. Observer cần kết thúc theo View đang được cập nhật. [Nguồn: LiveData API](https://developer.android.com/reference/androidx/lifecycle/LiveData).

### 5.2. value/setValue và postValue

| API | Cách dùng |
| --- | --- |
| `_data.value = value` / setValue | Main thread; cập nhật trực tiếp |
| `_data.postValue(value)` | Gửi cập nhật về main thread; dùng từ worker thread |
| `observe(owner)` | Tự điều chỉnh theo lifecycle |
| `observeForever(observer)` | Luôn active; phải chủ động removeObserver |

Nhiều `postValue()` trước khi main thread xử lý có thể được gộp, chỉ giá trị cuối được giao. Trộn `postValue` và `setValue` có thể khiến giá trị post được áp dụng sau giá trị set. Không dùng LiveData như hàng đợi đếm mọi event. [Nguồn: MutableLiveData API](https://developer.android.com/reference/androidx/lifecycle/MutableLiveData).

### 5.3. Transformation và nhiều nguồn

```kotlin
// Trong ViewModel; import androidx.lifecycle.map:
val countLabel: LiveData<String> = count.map { number -> "Count: $number" }
```

- `map`: đổi một giá trị sang kiểu hiển thị/kiểu dữ liệu khác.
- `switchMap`: đổi sang nguồn LiveData mới khi đầu vào đổi, ngừng theo nguồn cũ.
- `MediatorLiveData`: kết hợp nhiều nguồn, chủ động tính kết quả.

Transformation/observer thường chạy trên main thread, nên tránh xử lý danh sách lớn hoặc tác vụ nặng ở đó. LiveData phù hợp ranh giới UI; Flow thuận tiện hơn cho stream và transformations ở data layer. [Nguồn: LiveData overview](https://developer.android.com/topic/libraries/architecture/livedata).

Ví dụ hai input hợp lệ để bật nút; đặt trong một ViewModel khác, cập nhật source trên main thread:

```kotlin
private val name = MutableLiveData("")
private val acceptedTerms = MutableLiveData(false)
private val _canSubmit = MediatorLiveData(false).apply {
    fun recalculate() {
        value = !name.value.isNullOrBlank() && acceptedTerms.value == true
    }
    addSource(name) { recalculate() }
    addSource(acceptedTerms) { recalculate() }
}
val canSubmit: LiveData<Boolean> = _canSubmit
```

Nguồn được Mediator theo dõi theo trạng thái active của nó. Nếu yêu cầu validation phức tạp, tách quy tắc thành hàm dễ kiểm thử. [Nguồn: MediatorLiveData API](https://developer.android.com/reference/androidx/lifecycle/MediatorLiveData).

## 6. Flow, StateFlow và SharedFlow

### 6.1. Cold Flow

`Flow<T>` là luồng giá trị bất đồng bộ. Flow tạo bằng `flow {}` thường là cold: producer chạy khi collect, và mỗi collector có thể chạy lại producer riêng. Không phải mọi giá trị có kiểu `Flow` đều cold; StateFlow/SharedFlow cũng triển khai Flow. [Nguồn: Kotlin flows on Android](https://developer.android.com/kotlin/flow).

```kotlin
val numbers: Flow<Int> = flow {
    emit(1)
    delay(100)
    emit(2)
}

// Trong một coroutine:
numbers.collect { value -> println(value) }
```

Snippet cần import từ `kotlinx.coroutines` và `kotlinx.coroutines.flow`. Collect cùng cold Flow hai lần có thể gọi network hai lần nếu producer chứa network; cần quyết định sharing có chủ đích.

### 6.2. Bảng phân biệt

| Tiêu chí | Flow thông thường | StateFlow | SharedFlow |
| --- | --- | --- | --- |
| Producer | Phụ thuộc cách tạo; flow builder thường cold | Hot | Hot |
| Giá trị ban đầu | Không bắt buộc | Bắt buộc | Không bắt buộc |
| Đọc giá trị hiện tại | Không có API value chung | Có value | Không có value như StateFlow |
| Collector mới | Chạy producer nếu cold | Nhận state mới nhất | Nhận replay đã cấu hình |
| Giá trị bằng nhau | Phụ thuộc pipeline | Gộp theo equals | Không tự gộp theo equals |
| Vai trò thường gặp | Stream dữ liệu, transformations | State hiện tại của màn hình | Broadcast có chính sách replay/buffer rõ |

StateFlow giữ state mới nhất; collector chậm có thể bỏ qua các cập nhật trung gian. Khi sửa state có list, tạo state/list mới thay vì mutate object cũ tại chỗ. [Nguồn: StateFlow API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/).

```kotlin
private val _uiState = MutableStateFlow(CounterUiState())
val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

fun increment() {
    _uiState.update { current -> current.copy(count = current.count + 1) }
}
```

Snippet cần `CounterUiState(val count: Int = 0)`. `update` giúp cập nhật atomic; lambda có thể được chạy lại khi cạnh tranh, nên không đặt side effect như gọi API trong lambda.

SharedFlow với `replay = 0` không lưu emission cho người subscribe về sau. `extraBufferCapacity` không tự giữ event cho lúc hoàn toàn không có subscriber; khi đó replay mới là phần có thể được giữ. Không xem SharedFlow là cơ chế bảo đảm event đến UI đúng một lần. [Nguồn: SharedFlow API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/).

### 6.3. Operators cần sử dụng được

| Operator | Dùng khi | Điều cần nhớ |
| --- | --- | --- |
| `map` | Đổi dữ liệu sang UI model | Không tự đổi dispatcher |
| `filter` | Bỏ emission không phù hợp | Không phải lọc item trong list nếu chưa viết logic đó |
| `combine` | Kết hợp giá trị mới nhất của nhiều nguồn | Chờ mỗi nguồn có giá trị đầu tiên |
| `debounce` | Giảm xử lý khi nhập nhanh | Có thể cần opt-in theo phiên bản coroutines |
| `distinctUntilChanged` | Bỏ giá trị liên tiếp bằng nhau | StateFlow vốn đã có equality conflation |
| `flatMapLatest` | Chuyển stream, hủy stream trước khi input mới đến | Hợp với tìm kiếm; cần cancellation hợp tác |
| `catch` | Xử lý exception upstream | Không bắt exception ở collector downstream |
| `flowOn` | Đổi context của upstream | Không đổi toàn bộ collector sang dispatcher đó |
| `stateIn` | Chia sẻ Flow thành StateFlow | Chọn scope, started policy, initial state |
| `shareIn` | Chia sẻ thành SharedFlow | Chọn scope, started policy, replay |

[Nguồn: Kotlin flows on Android](https://developer.android.com/kotlin/flow), [flowOn API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flow-on.html), [catch API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html).

Mô hình tìm kiếm API, chưa phải code đầy đủ:

```text
query → debounce → distinctUntilChanged → flatMapLatest(repository.search)
      → map sang UI state → catch lỗi dự kiến → stateIn
```

Khi cần tiếp tục tìm sau một request lỗi, thường xử lý lỗi **bên trong stream của từng query**. Một `catch` ở cuối outer stream có thể phát state lỗi rồi hoàn tất pipeline; input query mới không còn được xử lý như mong muốn. `flatMapLatest` hủy collection trước khi chuyển nguồn mới, không bảo đảm API blocking bất kỳ sẽ dừng được. [Nguồn: flatMapLatest API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html), [catch API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html).

### 6.4. stateIn và WhileSubscribed

```kotlin
val uiState = upstream.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
    initialValue = ScreenUiState()
)
```

Scope xác định nơi chạy coroutine chia sẻ; started policy xác định khi nào collect upstream. WhileSubscribed bắt đầu khi có subscriber, chờ timeout sau subscriber cuối rồi dừng upstream. 5 giây là lựa chọn mẫu để giảm restart khi xoay, không phải giá trị bắt buộc. Mặc định replay cache được giữ; subscriber trở lại có thể nhận state cũ trước khi upstream cập nhật. [Nguồn: StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow), [WhileSubscribed API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-sharing-started/-companion/-while-subscribed.html).

Dừng upstream của `stateIn` không đồng nghĩa hủy ViewModel hoặc một job `refresh()` độc lập trong viewModelScope. Nếu muốn công việc gắn trực tiếp với subscriber, phải thiết kế producer theo cơ chế đó.

## 7. Quan sát Flow đúng lifecycle trong Fragment

```kotlin
// Trong onViewCreated(); import lifecycleScope, repeatOnLifecycle, launch, collect.
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}
```

Khi View xuống dưới STARTED, block collection bị cancel; khi trở lại STARTED, một block mới chạy. Khi View bị hủy, coroutine ở lifecycleScope của View kết thúc. StateFlow vẫn còn state; collector UI mới nhận state mới nhất. [Nguồn: Architecture recommendations for Views](https://developer.android.com/topic/architecture/views/recommendations-views).

`lifecycleScope.launch { collect(...) }` một mình chỉ hủy khi owner bị destroy, không dừng ở STOPPED. Trong Fragment, không collect UI bằng `viewModelScope` vì scope đó có thể sống lâu hơn View.

Nếu collect hai stream không kết thúc, cần hai coroutine con:

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.profile.collect { renderProfile(it) } }
        launch { viewModel.notifications.collect { renderNotifications(it) } }
    }
}
```

Viết `profile.collect` rồi `notifications.collect` nối tiếp có thể khiến collector thứ hai không bao giờ bắt đầu. Nếu các nguồn tạo thành cùng một state màn hình, cân nhắc `combine` trong ViewModel để UI chỉ collect một uiState.

## 8. LiveData và Flow: chọn và chuyển đổi

| Nội dung | LiveData | Flow/StateFlow |
| --- | --- | --- |
| Nhận biết Android lifecycle | observe(owner) có sẵn | Collector UI phải gắn lifecycle rõ |
| Coroutine/operator | Có integration và transformations | Nhiều operator cho stream bất đồng bộ |
| State hiện tại | value có thể chưa được set | StateFlow bắt buộc initial value |
| Giá trị trùng | setValue có thể thông báo lại observer active | StateFlow gộp theo equals |
| Ranh giới sử dụng | UI Views trong dự án đang dùng LiveData | Data layer và UI state theo kiến trúc Flow |

Trong bài mới, chọn Flow ở repository và StateFlow ở ViewModel; vẫn cần dùng được LiveData để làm việc trong dự án có sẵn. Không cần duy trì hai mutable nguồn state độc lập cho cùng một dữ liệu. [Nguồn: LiveData overview](https://developer.android.com/topic/libraries/architecture/livedata), [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow).

```kotlin
// Trong TasksViewModel ở phần 9; thêm dependency LiveData KTX và import asLiveData.
val uiStateLiveData: LiveData<TasksUiState> = uiState.asLiveData()

// Trong Fragment: dùng thay cho collector Flow, không chạy cả hai để render cùng UI.
viewModel.uiStateLiveData.observe(viewLifecycleOwner) { state ->
    render(state)
}
```

`asLiveData()` có cơ chế active/inactive và timeout của coroutine bridge; không giả định producer dừng ngay khi observer inactive. `asFlow()` có thể chuyển LiveData sang Flow khi cần. Kiểm tra ownership và sharing khi ghép các bridge để tránh producer chạy nhiều lần. [Nguồn: Lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines).

## 9. Ví dụ xuyên suốt: danh sách bài học MVVM

### 9.1. Yêu cầu và cấu trúc

Màn hình tải danh sách, nhập từ khóa để lọc cục bộ, có Refresh, Loading, Error và Empty. ViewModel giữ từ khóa qua xoay/saved state. Repository fake tạo dữ liệu bằng delay, không cần quyền mạng.

```text
java/com/example/mvvmlesson/
├── model/LessonTask.kt
├── data/TasksRepository.kt
├── ui/TasksUiState.kt
├── ui/TasksViewModel.kt
├── ui/TasksFragment.kt
└── MainActivity.kt
res/layout/activity_main.xml
res/layout/fragment_tasks.xml
res/values/strings.xml
```

Trong ví dụ, các class cùng package `com.example.mvvmlesson` dù file được nhóm bằng folder. Khi dùng package con thật, thêm import tương ứng.

### 9.2. Model và repository

File `LessonTask.kt`:

```kotlin
package com.example.mvvmlesson

data class LessonTask(val id: Long, val title: String)
```

File `TasksRepository.kt`:

```kotlin
package com.example.mvvmlesson

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TasksRepository {
    fun observeTasks(): Flow<List<LessonTask>>
    suspend fun refresh()
}

class FakeTasksRepository(
    private var failNextRefresh: Boolean = false
) : TasksRepository {
    private val tasks = MutableStateFlow<List<LessonTask>>(emptyList())

    override fun observeTasks(): Flow<List<LessonTask>> = tasks.asStateFlow()

    override suspend fun refresh() {
        delay(700) // Mô phỏng I/O suspend; không dùng Thread.sleep trên main.
        if (failNextRefresh) {
            failNextRefresh = false
            throw IOException("Simulated refresh failure")
        }
        tasks.value = listOf(
            LessonTask(1, "Học vòng đời ViewModel"),
            LessonTask(2, "Thực hành LiveData"),
            LessonTask(3, "Thực hành StateFlow")
        )
    }
}
```

Repository không biết TextView hoặc Fragment. `observeTasks()` trả kiểu Flow nhưng nguồn thực tế là hot StateFlow; đây là lý do không kết luận cold/hot chỉ từ tên kiểu.

### 9.3. UI state

File `TasksUiState.kt`:

```kotlin
package com.example.mvvmlesson

enum class TasksError { REFRESH_FAILED }

data class TasksUiState(
    val query: String = "",
    val tasks: List<LessonTask> = emptyList(),
    val isLoading: Boolean = false,
    val error: TasksError? = null
)
```

State có thể vừa chứa list cũ vừa có `isLoading = true` khi refresh. Empty được UI suy ra khi không loading, không error và list rỗng.

### 9.4. ViewModel: state, action, scope và saved query

File `TasksViewModel.kt`:

```kotlin
package com.example.mvvmlesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
    private val repository: TasksRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private data class RefreshStatus(
        val isLoading: Boolean = false,
        val error: TasksError? = null
    )

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val refreshStatus = MutableStateFlow(RefreshStatus())
    private var refreshJob: Job? = null

    val currentQuery: String get() = query.value

    val uiState: StateFlow<TasksUiState> = combine(
        repository.observeTasks(), query, refreshStatus
    ) { tasks, currentQuery, status ->
        val keyword = currentQuery.trim()
        TasksUiState(
            query = currentQuery,
            tasks = tasks.filter { it.title.contains(keyword, ignoreCase = true) },
            isLoading = status.isLoading,
            error = status.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = TasksUiState(query = currentQuery, isLoading = true)
    )

    init {
        refresh()
    }

    fun setQuery(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return // Không chồng nhiều request refresh.
        refreshJob = viewModelScope.launch {
            refreshStatus.value = RefreshStatus(isLoading = true)
            try {
                repository.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                refreshStatus.update { it.copy(error = TasksError.REFRESH_FAILED) }
            } finally {
                refreshStatus.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        private const val KEY_QUERY = "task_query"
    }
}
```

`refresh()` nằm trong init nên một instance ViewModel gọi lần tải đầu một lần. Xoay và tạo View UI mới không tự tạo request đầu mới; process recreation tạo ViewModel mới thì tải lại. Bài fake chỉ có refresh ném IOException; stream observe không ném lỗi. Với stream production, cần chính sách lỗi/retry upstream riêng trước khi stateIn.

### 9.5. Layout và strings

File `res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.fragment.app.FragmentContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/activityContainer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

File `res/layout/fragment_tasks.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    <EditText
        android:id="@+id/queryInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/query_hint"
        android:inputType="text"
        android:maxLines="1" />
    <Button
        android:id="@+id/refreshButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/refresh" />
    <ProgressBar
        android:id="@+id/loadingIndicator"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:contentDescription="@string/loading"
        android:visibility="gone" />
    <TextView
        android:id="@+id/errorText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="gone" />
    <TextView
        android:id="@+id/resultsText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```

File `res/values/strings.xml`; nếu file đã có, thêm item vào resources hiện tại:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MVVM Lesson</string>
    <string name="query_hint">Tìm bài học</string>
    <string name="refresh">Tải lại</string>
    <string name="loading">Đang tải dữ liệu</string>
    <string name="refresh_failed">Không thể tải dữ liệu. Hãy thử lại.</string>
    <string name="empty_results">Không có bài học phù hợp</string>
</resources>
```

### 9.6. Fragment: nhận action và render

File `TasksFragment.kt`:

```kotlin
package com.example.mvvmlesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private val viewModel: TasksViewModel by viewModels {
        viewModelFactory {
            initializer {
                TasksViewModel(
                    repository = FakeTasksRepository(),
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val queryInput = view.findViewById<EditText>(R.id.queryInput)
        val refreshButton = view.findViewById<Button>(R.id.refreshButton)
        val loading = view.findViewById<ProgressBar>(R.id.loadingIndicator)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val resultsText = view.findViewById<TextView>(R.id.resultsText)

        // Khôi phục input từ state nguồn trước khi gắn TextWatcher.
        queryInput.setText(viewModel.currentQuery)
        queryInput.doAfterTextChanged { text ->
            viewModel.setQuery(text?.toString().orEmpty())
        }
        refreshButton.setOnClickListener { viewModel.refresh() }

        fun render(state: TasksUiState) {
            loading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            refreshButton.isEnabled = !state.isLoading
            errorText.visibility = if (state.error != null) View.VISIBLE else View.GONE
            errorText.text = when (state.error) {
                TasksError.REFRESH_FAILED -> getString(R.string.refresh_failed)
                null -> ""
            }
            resultsText.text = when {
                state.tasks.isNotEmpty() -> state.tasks.joinToString("\n") { it.title }
                state.isLoading -> getString(R.string.loading)
                state.error != null -> ""
                else -> getString(R.string.empty_results)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }
}
```

Factory nhận CreationExtras của owner và tạo SavedStateHandle có cơ chế restore. `SavedStateHandle()` tự tạo trong UI không thay thế được wiring này. Repository fake được tạo khi factory tạo ViewModel mới; dự án thật nên lấy repository từ app container/DI để chia sẻ và quản lý data layer theo thiết kế. [Nguồn: Create ViewModels with dependencies](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories).

Fragment không lọc list hoặc bắt lỗi network. Hàm render chỉ cập nhật UI; không gọi refresh. Input được khôi phục từ `currentQuery`, không gán lại từ mỗi emission của uiState: tránh vòng lặp TextWatcher và tránh ghi đè thao tác nhập bằng state replay cũ.

### 9.7. Activity host

File `MainActivity.kt`:

```kotlin
package com.example.mvvmlesson

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.activityContainer, TasksFragment::class.java, null)
                .commit()
        }
    }
}
```

Manifest khai báo MainActivity và theme tương thích AppCompat. Sau recreation, để FragmentManager restore Fragment; không thêm lại vô điều kiện.

### 9.8. Những điều cần quan sát

1. Mở màn hình: factory tạo ViewModel, init gọi refresh, UI nhận loading rồi list.
2. Nhập `LiveData`: action đổi SavedStateHandle; combine tạo list đã lọc.
3. Xoay: View mới đăng ký collector mới; ViewModel và request đang chạy được giữ.
4. Background: UI collector dừng; job refresh đang chạy không tự bị hủy.
5. Không còn subscriber quá 5 giây: combine upstream dừng; refresh độc lập vẫn có thể chạy.
6. Quay lại: upstream được collect lại, kết hợp dữ liệu mới nhất từ repository/query/status.
7. Đổi factory thành `FakeTasksRepository(failNextRefresh = true)`: lần đầu lỗi, Refresh sau đó thành công.

**Giới hạn:** TextView hiển thị list để tập trung MVVM; chưa dùng RecyclerView, API hoặc DB. Tìm kiếm là lọc cục bộ, chưa debounce/network search. Filter nhỏ chạy trên main trong ViewModel; danh sách lớn cần xử lý thích hợp. Danh sách fake mất theo process; chỉ query được lưu bằng SavedStateHandle.

## 10. Thực hành riêng với LiveData

### 10.1. Counter hoàn chỉnh

```kotlin
package com.example.mvvmlesson

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CounterViewModel : ViewModel() {
    private val _count = MutableLiveData(0)
    val count: LiveData<Int> = _count

    fun increment() {
        _count.value = (_count.value ?: 0) + 1
    }

    fun reset() {
        _count.value = 0
    }
}
```

Tạo Fragment có TextView và hai nút; lấy `CounterViewModel by viewModels()`, observe bằng viewLifecycleOwner. Xoay và kiểm tra counter không mất; remove Fragment vĩnh viễn rồi tạo mới thì scope mới có counter mới. Counter mẫu chưa dùng SavedStateHandle nên không bảo đảm qua process death.

### 10.2. Tải dữ liệu với LiveData

Trong một ViewModel khác có repository suspend và UI state tương tự, có thể cập nhật LiveData từ viewModelScope:

```kotlin
private val _state = MutableLiveData(TasksUiState())
val state: LiveData<TasksUiState> = _state

fun refresh() {
    viewModelScope.launch {
        _state.value = TasksUiState(isLoading = true)
        try {
            val tasks = repository.fetchTasks() // suspend main-safe, trả List.
            _state.value = TasksUiState(tasks = tasks)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            _state.value = TasksUiState(error = TasksError.REFRESH_FAILED)
        }
    }
}
```

Đây là **snippet cho repository có hàm fetchTasks()**, khác interface ở phần 9 chỉ có refresh/observeTasks. Cần bổ sung chống request chồng nhau theo yêu cầu. Coroutine mặc định trở về main sau suspend main-safe, nên dùng value; không cần postValue chỉ vì có gọi hàm suspend.

### 10.3. Bài so sánh

Chạy màn hình Tasks bằng collector Flow. Sau đó thêm `uiState.asLiveData()` và chuyển UI sang observe LiveData, bỏ collector Flow tương ứng. So sánh active/inactive, replay state khi quay lại và số lần repository được gọi. Cả hai cách phải giữ đúng separation giữa View và ViewModel.

## 11. UI event và các lỗi thiết kế thường gặp

State như “list hiện tại”, “đang loading”, “lỗi cần hiển thị” phải render lại được. Một Toast trong observer có thể hiện lại khi View mới nhận state cũ. Với thông báo cần xác nhận đã hiển thị, biểu diễn state với ID và action tiêu thụ rõ; không mặc định một SingleLiveEvent/SharedFlow sẽ bảo đảm giao đúng một lần qua mọi lifecycle. [Nguồn: UI events for Views](https://developer.android.com/topic/architecture/views/ui-layer/events-views).

Trong bài mẫu, lỗi được hiển thị inline; render lại không tạo hành động nghiệp vụ mới. Điều hướng do UI xử lý dựa trên luồng action/state đã thiết kế, ViewModel không giữ NavController hoặc Fragment.

| Lỗi | Cách sửa |
| --- | --- |
| ViewModel giữ Binding/Fragment | Chỉ giữ state/dependency không tham chiếu UI |
| Khởi tạo ViewModel trực tiếp trong UI | Dùng provider/delegate với owner đúng |
| Mọi Fragment dùng activityViewModels | Chỉ dùng shared scope khi state thật sự cần chia sẻ |
| Gọi API trong render/collect mỗi emission | Gọi qua action/init/pipeline producer có chủ đích |
| Dùng viewModelScope collect vào TextView | UI collect theo View lifecycle |
| LiveData observe với owner sống lâu hơn View | Dùng viewLifecycleOwner |
| Cho rằng suspend tự chạy background | Đảm bảo main-safe ở nơi thực hiện blocking work |
| Bắt mọi exception và nuốt cancellation | Rethrow cancellation; xử lý lỗi dự kiến cụ thể |
| Mutate list bên trong StateFlow tại chỗ | Tạo list/state mới, dùng copy/update |
| Dùng StateFlow để đếm mọi event | State có conflation; chọn cơ chế phù hợp yêu cầu |
| Collect hai Flow nối tiếp | launch riêng hoặc combine |
| stateIn WhileSubscribed không có collector trong test | Tạo subscriber trong test để upstream chạy |
| Query API cũ ghi đè query mới | Dùng latest/cancellation hoặc cơ chế loại kết quả cũ |
| Nghĩ ViewModel sống qua process death | Lưu state nhỏ; persist dữ liệu cần bền vững |

## 12. Bài tập, kiểm thử và đánh giá

### 12.1. Bài tập từng phần

| Bài | Yêu cầu | Tiêu chí đạt |
| --- | --- | --- |
| A — Tách MVVM | Chuyển lọc list/tải dữ liệu khỏi Fragment | View chỉ nhận action/render; repository không biết UI |
| B — Owner | Counter riêng, shared ở Activity và shared ở parent | Giải thích đúng instance nào được chia sẻ |
| C — Scope | Tải đang chạy rồi xoay/background/remove | Phân biệt dừng collector với hủy job ViewModel |
| D — LiveData | Counter, map và validation hai nguồn | Mutable private; owner đúng; hiểu value/postValue |
| E — Flow | Filter/combine/stateIn | Đọc được pipeline và giải thích cold/hot |
| F — API search | Query debounce và flatMapLatest | Kết quả cũ không ghi đè; sau lỗi vẫn tìm được |
| G — Restore | Query và ID chọn bằng SavedStateHandle | Khôi phục state nhỏ; tải lại data khi cần |

### 12.2. Bài tổng hợp: Task Explorer

Nâng ví dụ thành RecyclerView với tìm kiếm, refresh/retry, trạng thái loading/empty/error. Dialog chọn bộ lọc từ bài trước trả Fragment Result; Fragment nhận rồi gọi `viewModel.setFilter(...)`. Hai child Fragment có thể dùng chung ViewModel của parent nếu cần chung bộ lọc.

Yêu cầu nghiệm thu:

- ViewModel có state công khai chỉ đọc; UI không tự thay đổi mutable state.
- Repository là nguồn dữ liệu; có fake cho loading, lỗi, thành công và dữ liệu rỗng.
- Coroutine dùng scope phù hợp, không block main, không nuốt cancellation.
- Flow được collect theo View lifecycle; LiveData dùng đúng owner.
- Có một phiên bản UI dùng Flow và một phiên bản dùng LiveData bridge để so sánh.
- Xoay không tạo request đầu trùng do khởi tạo lại View; giữ query/ID theo thiết kế.
- Process recreation tạo instance mới và khôi phục state đã lưu phù hợp.
- UI render lại không tự thực hiện nghiệp vụ hoặc điều hướng lặp.

### 12.3. Ma trận kiểm tra thủ công

| Tình huống | Kết quả mong đợi |
| --- | --- |
| Tải lần đầu thành công | Loading → list; nút hoạt động lại |
| Lần đầu lỗi, bấm retry | Error → loading → list |
| Dữ liệu rỗng / query không khớp | Empty rõ ràng, không nhầm loading/error |
| Click refresh liên tiếp | Không chồng request theo chính sách mẫu |
| Xoay lúc đang tải | Không tạo lại ViewModel/request đầu; UI mới nhận kết quả |
| Background lúc đang tải | Collector UI dừng; refresh được tiếp tục theo ViewModel scope |
| View bị hủy nhưng Fragment còn back stack | Không cập nhật View cũ; state có thể còn trong ViewModel |
| Owner bị remove/finish | ViewModel clear; coroutine hợp tác bị hủy |
| Không có subscriber quá timeout | stateIn ngừng upstream theo WhileSubscribed |
| Nhập nhanh ở bản API search | Dữ liệu phù hợp query hiện tại, không lấy kết quả cũ |
| LiveData post nhiều giá trị nhanh | Giải thích được khả năng gộp giá trị |
| Process recreation có saved state | Query phục hồi; repository/data được tạo/tải lại |
| Shared ViewModel | Chỉ các màn hình cùng owner/key chia sẻ instance |

### 12.4. Kiểm thử ViewModel với fake

Ưu tiên kiểm tra hành vi: loading → success/error, retry, lọc đúng, bảo vệ request trùng và không chuyển cancellation thành lỗi UI. Khi dùng kotlinx-coroutines-test, thay Main dispatcher bằng test dispatcher trước khi tạo ViewModel, dùng cùng scheduler và reset Main sau test. [Nguồn: Testing Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines/test).

Với `stateIn(WhileSubscribed)`, cần collector để upstream bắt đầu; chỉ đọc `.value` không tự subscribe. Dùng backgroundScope cho collector của stream không kết thúc và điều khiển thời gian ảo cho delay/timeout. Dọn ViewModelStore/scope test để không để job tồn tại sau test. [Nguồn: Testing Kotlin flows](https://developer.android.com/kotlin/flow/test).

Với LiveData trong unit test JVM, dùng công cụ chạy Architecture Components đồng bộ như InstantTaskExecutorRule; observerForever trong test vẫn cần gỡ sau test. Kiểm thử lifecycle/Fragment recreation bổ sung bằng công cụ Android thích hợp, không xem unit test ViewModel là đủ để chứng minh UI owner đúng.

| Hạng mục | Điểm |
| --- | ---: |
| Tách trách nhiệm MVVM và thiết kế state | 20 |
| ViewModel owner/lifecycle và SavedStateHandle | 20 |
| viewModelScope, main-safe, cancellation/lỗi | 20 |
| LiveData, observe và transformations | 15 |
| Flow/StateFlow và collection theo lifecycle | 20 |
| Fake repository và kiểm chứng hành vi | 5 |
| **Tổng** | **100** |

Mức đạt đề xuất: từ 75 điểm. Cần sửa lỗi cập nhật View đã hủy, block main thread, giữ UI trong ViewModel và nuốt cancellation trước khi nghiệm thu.

## 13. Câu hỏi ôn tập và đáp án ngắn

1. **Model có phải chỉ là data class?** Không; còn dữ liệu, repository, nguồn dữ liệu và logic liên quan.
2. **MVVM có bắt buộc Data Binding/Hilt?** Không; đó là công cụ tùy chọn.
3. **ViewModel có nên giữ Fragment/Binding?** Không; tuổi thọ có thể dài hơn UI.
4. **Hai Fragment cùng class ViewModel có chắc dùng chung?** Không; phụ thuộc owner và key.
5. **Xoay màn hình có tạo ViewModel mới không?** Thường không nếu lấy đúng qua store của owner.
6. **onDestroyView có hủy ViewModel luôn không?** Không.
7. **ViewModel sống qua process death không?** Không; instance mới có thể nhận saved state.
8. **viewModelScope tự chạy IO không?** Không; mặc định main dispatcher trên Android.
9. **Coroutine trong viewModelScope bị hủy lúc STOPPED?** Không chỉ vì STOPPED; bị hủy khi ViewModel clear.
10. **SupervisorJob có tự chặn crash không?** Không; vẫn phải xử lý lỗi dự kiến.
11. **Vì sao phải rethrow CancellationException?** Để giữ đúng cancellation của coroutine.
12. **LiveData observe UI Fragment dùng owner nào?** viewLifecycleOwner.
13. **postValue có bảo đảm giao mọi giá trị không?** Không; có thể gộp trước khi main xử lý.
14. **Flow có luôn cold không?** Không; phụ thuộc nguồn, StateFlow/SharedFlow là hot.
15. **StateFlow có giao mọi state trung gian không?** Không; có conflation và equality suppression.
16. **repeatOnLifecycle làm gì?** Hủy block dưới trạng thái yêu cầu, chạy lại khi owner đạt trạng thái đó.
17. **catch cuối Flow có bắt lỗi trong collect không?** Không; catch xử lý upstream.
18. **WhileSubscribed dừng thì mọi job ViewModel cũng dừng?** Không; job độc lập vẫn theo scope của nó.
19. **SharedFlow replay 0 có giữ event khi UI chưa subscribe không?** Không.
20. **UI nhận state có nên tự gọi API lại?** Không; render không tự phát sinh nghiệp vụ lặp.

## 14. Checklist tự đánh giá

- [ ] Tôi tách được View, ViewModel và repository, không chỉ chuyển code vào class mới.
- [ ] Tôi thiết kế UI state và action đủ rõ để render lại màn hình.
- [ ] Tôi chọn đúng ViewModelStoreOwner và giải thích shared scope.
- [ ] Tôi phân biệt configuration change, View destruction và process death.
- [ ] Tôi dùng SavedStateHandle cho state nhỏ, storage cho dữ liệu bền vững.
- [ ] Tôi dùng viewModelScope, dispatcher và cancellation đúng mục đích.
- [ ] Tôi không giữ UI trong ViewModel hoặc dùng GlobalScope để né lifecycle.
- [ ] Tôi đóng gói MutableLiveData và observe bằng owner đúng.
- [ ] Tôi hiểu value/postValue, map/switchMap và MediatorLiveData.
- [ ] Tôi phân biệt Flow, StateFlow, SharedFlow và cold/hot.
- [ ] Tôi dùng combine/latest/catch/flowOn/stateIn có chủ đích.
- [ ] Tôi collect UI theo View lifecycle bằng repeatOnLifecycle.
- [ ] Tôi hiểu state replay, conflation và giới hạn của cơ chế event.
- [ ] Tôi kiểm tra loading/error/empty/retry, xoay và restore bằng fake data.

## 15. Hướng dẫn dùng nguồn

Nguồn chính là **Android Developers của Google**; hành vi coroutines/Flow được đối chiếu thêm với **Kotlin/kotlinx.coroutines của JetBrains**. Các link được đặt tại phần liên quan. Giải thích tiếng Việt, lộ trình, ví dụ và bài tập được biên soạn cho intern, không phải bản dịch nguyên văn.

Khi đọc một màn hình MVVM, lần theo: **action từ View → hàm ViewModel → repository → state → observer/collector → render**. Sau đó xác định **owner của ViewModel, scope của coroutine và lifecycle của collector** để giải thích hành vi khi xoay, background hoặc rời màn hình.
