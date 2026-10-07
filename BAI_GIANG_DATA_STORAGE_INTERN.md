# Bài giảng Android: Data Storage — Level Intern

**Đối tượng:** intern đã biết Kotlin, coroutine, Fragment, MVVM và RecyclerView.  
**Phạm vi:** SharedPreferences, Preferences DataStore, Room, SQL và Paging 3 trong ứng dụng Android Views.  
**Thời lượng gợi ý:** 5 buổi × 3 giờ, cộng thời gian thực hành.  
**Ngày đối chiếu nguồn:** 06/10/2026.  
**Bài trước:** [ViewPager, ViewPager2 và TabLayout](BAI_GIANG_VIEWPAGER_TABLAYOUT_INTERN.md).  
**Ôn tập:** [MVVM](BAI_GIANG_MVVM_INTERN.md), [RecyclerView](BAI_GIANG_RECYCLERVIEW_INTERN.md).

“Preferences” trong bài bao gồm SharedPreferences để đọc/bảo trì code hiện có và Preferences DataStore cho phần thực hành mới. “RoomDB” được hiểu là cơ sở dữ liệu SQLite truy cập qua thư viện Room. “Observer data” là quan sát kết quả truy vấn thay đổi bằng LiveData hoặc Flow.

Ví dụ dùng package `com.example.storagelesson`; thay bằng package/namespace thực tế. Các class có tên file ghép thành phần dữ liệu và màn hình danh sách ghi chú mẫu. Snippet mở rộng được ghi rõ vị trí tích hợp. Tài liệu không thay thế cấu hình Gradle, manifest và Activity của một ứng dụng Android hoàn chỉnh.

**Trạng thái kiểm chứng:** kiểm tra tĩnh Markdown/XML/resource và chạy các truy vấn SQL mẫu trên SQLite với dữ liệu kiểm chứng. Kotlin, code sinh bởi Room, migration validation của Room và UI chưa build/chạy vì workspace chỉ có tài liệu, chưa có project Android/Gradle. Phần 14 hướng dẫn kiểm thử khi triển khai.

## 1. Mục tiêu và lộ trình

| Nội dung | Intern cần làm được |
| --- | --- |
| Preferences | Đọc/ghi/xóa, giá trị mặc định, observe, migration và xử lý lỗi |
| Room | Thiết kế Entity/DAO/Database, CRUD, transaction và nâng schema |
| Custom query | Viết WHERE, LIKE, IN, projection, UPDATE và query động đúng cách |
| Observable data | Phân biệt suspend, LiveData và Flow; collect theo lifecycle |
| Pagination | Hiểu LIMIT/OFFSET, keyset và triển khai Paging 3 với Room |
| Sort | Sort bằng SQL trên toàn dataset; thứ tự ổn định với tie-breaker |
| Group | Phân biệt GROUP BY thống kê, quan hệ dữ liệu và nhóm item trên UI |

| Buổi | Nội dung | Thực hành |
| --- | --- | --- |
| 1 | SharedPreferences, DataStore | Lưu lựa chọn sort/hiện ghi chú đã lưu trữ; migration |
| 2 | Room, schema và CRUD | Database ghi chú, DAO và repository |
| 3 | Query và observation | Tìm kiếm, filter, LiveData/Flow; cập nhật UI tự động |
| 4 | Pagination, sort và group | PagingDataAdapter, thống kê theo category |
| 5 | Migration, lỗi và kiểm chứng | Nâng DB v1→v2, test kết quả SQL và state sau restart |

## 2. Chọn nơi lưu theo loại dữ liệu

| Dữ liệu | Nơi lưu phù hợp | Lý do |
| --- | --- | --- |
| Sort mặc định, theme, đã xem onboarding | Preferences DataStore | Tập key–value nhỏ, đọc bằng Flow |
| Cấu hình có schema kiểu dữ liệu rõ | Typed DataStore | Có serializer/schema theo giải pháp được chọn |
| Ghi chú, task, danh mục, quan hệ và truy vấn | Room | Cần filter, sort, index, transaction, cập nhật từng dòng |
| Ảnh/video/file tải về | File storage | Lưu file; Room có thể giữ metadata/đường dẫn |
| Giá trị tạm trong phiên màn hình | ViewModel/SavedStateHandle tùy yêu cầu | Không phải mọi state đều cần ghi xuống disk |

Không lưu hàng nghìn ghi chú thành một chuỗi JSON trong preferences rồi tải toàn bộ để sort. Room cũng không tự mã hóa database; MODE_PRIVATE của preferences kiểm soát truy cập app, không đồng nghĩa dữ liệu được mã hóa. Với dữ liệu nhạy cảm cần một thiết kế lưu trữ phù hợp yêu cầu sản phẩm.

DataStore phù hợp tập dữ liệu nhỏ; Room phù hợp khi cần truy vấn, cập nhật một phần và quan hệ. [Nguồn: DataStore overview](https://developer.android.com/topic/libraries/architecture/datastore), [Room overview](https://developer.android.com/training/data-storage/room).

```text
Fragment: gửi action, render state và collect theo View lifecycle
                          │
                          ▼
                       ViewModel
                 ┌────────┴──────────┐
                 ▼                   ▼
        SettingsRepository     NotesRepository
                 │                   │
                 ▼                   ▼
       Preferences DataStore      Room DAO
          sort / filter        ghi chú / thống kê
                 └──────── dữ liệu → UI ────────┘
```

Preferences và Room là hai kho riêng. `DataStore.edit` và một Room transaction không tạo thành transaction chung; thao tác cần atomic giữa các dữ liệu nên được thiết kế cùng kho hoặc có cơ chế phối hợp riêng.

## 3. SharedPreferences: thao tác và giới hạn

### 3.1 Đọc/ghi/xóa với key có quy ước

`StorageModels.kt` — dùng chung cho các phần sau:

```kotlin
package com.example.storagelesson

const val LEGACY_SETTINGS_FILE = "lesson_settings"

object SettingsKeys {
    const val SORT = "sort_order"
    const val INCLUDE_ARCHIVED = "include_archived"
}

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST, TITLE_ASC;

    companion object {
        fun fromStored(value: String?): SortOrder =
            values().firstOrNull { it.name == value } ?: NEWEST_FIRST
    }
}

data class UserSettings(
    val sort: SortOrder = SortOrder.NEWEST_FIRST,
    val includeArchived: Boolean = false
)

data class NoteFilter(
    val pattern: String,
    val category: String?,
    val sort: SortOrder,
    val includeArchived: Boolean
)

fun containsPattern(input: String): String {
    val escaped = input.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%" + escaped + "%"
}
```

Enum được lưu bằng name ổn định, không dùng ordinal vì thêm/đổi thứ tự enum có thể làm dữ liệu cũ mang ý nghĩa khác. Nếu đổi tên giá trị đã lưu, cần quy tắc migration/alias; fallback chỉ là một phần xử lý tương thích.

`LegacySettings.kt`:

```kotlin
package com.example.storagelesson

import android.content.Context
import android.content.SharedPreferences

class LegacySettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        LEGACY_SETTINGS_FILE, Context.MODE_PRIVATE
    )

    fun read(): UserSettings = UserSettings(
        sort = SortOrder.fromStored(prefs.getString(SettingsKeys.SORT, null)),
        includeArchived = prefs.getBoolean(SettingsKeys.INCLUDE_ARCHIVED, false)
    )

    fun save(settings: UserSettings) {
        prefs.edit()
            .putString(SettingsKeys.SORT, settings.sort.name)
            .putBoolean(SettingsKeys.INCLUDE_ARCHIVED, settings.includeArchived)
            .apply()
    }

    fun removeSort() {
        prefs.edit().remove(SettingsKeys.SORT).apply()
    }

    fun clearDemoSettings() {
        prefs.edit().clear().apply()
    }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
```

`clear()` xóa mọi key trong file đang mở, không chỉ một key. Chỉ dùng khi đúng yêu cầu reset. Nếu đổi kiểu của key đã lưu, getter không tự convert kiểu cũ; có thể gặp ClassCastException.

### 3.2 apply và commit

| API | Hành vi cần nhớ | Cách dùng |
| --- | --- | --- |
| `apply()` | Đổi dữ liệu trong memory ngay, ghi disk bất đồng bộ, không trả kết quả ghi | Thường dùng trong code SharedPreferences hiện có |
| `commit()` | Ghi đồng bộ và trả Boolean | Khi cần biết kết quả ghi; chạy ngoài main thread |

Không suy ra “apply hoàn toàn không thể gây chậm UI” chỉ vì phần ghi chạy bất đồng bộ; tránh ghi quá thường xuyên. Không dùng read–modify–write rời rạc làm bộ đếm cần atomic khi nhiều coroutine/thread cùng cập nhật. [Nguồn: SharedPreferences guide](https://developer.android.com/training/data-storage/shared-preferences).

Khi dùng listener, giữ instance để gỡ ở lifecycle tương ứng. Listener không tự lifecycle-aware; sự kiện key đổi cũng không chứng minh dữ liệu đã bền vững trên disk. Ví dụ tích hợp trong host, không phải class độc lập:

```kotlin
private val preferenceListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SettingsKeys.SORT || key == SettingsKeys.INCLUDE_ARCHIVED) {
            // Đọc lại snapshot và render hoặc chuyển vào tầng state.
        }
    }
// Khi bắt đầu quan sát: legacy.register(preferenceListener)
// Khi kết thúc: legacy.unregister(preferenceListener)
```

Preferences APIs dùng để xây UI settings là khái niệm khác với API lưu key–value. Trong code mới của bài này, dùng Preferences DataStore theo hướng dẫn Android Developers.

## 4. Preferences DataStore: đọc Flow, ghi suspend và migration

### 4.1 Instance, key và edit

`SettingsRepository.kt`:

```kotlin
package com.example.storagelesson

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.lessonSettingsStore by preferencesDataStore(
    name = "lesson_settings_v2",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SETTINGS_FILE))
    }
)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.lessonSettingsStore
    private val sortKey = stringPreferencesKey(SettingsKeys.SORT)
    private val includeArchivedKey = booleanPreferencesKey(SettingsKeys.INCLUDE_ARCHIVED)

    val settings: Flow<UserSettings> = store.data
        .catch { error ->
            if (error is IOException) {
                Log.e("StorageLesson", "Cannot read demo settings", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            UserSettings(
                sort = SortOrder.fromStored(prefs[sortKey]),
                includeArchived = prefs[includeArchivedKey] ?: false
            )
        }
        .distinctUntilChanged()

    suspend fun setSort(sort: SortOrder) {
        store.edit { prefs -> prefs[sortKey] = sort.name }
    }

    suspend fun setIncludeArchived(include: Boolean) {
        store.edit { prefs -> prefs[includeArchivedKey] = include }
    }

    suspend fun reset() {
        store.edit { prefs ->
            prefs.remove(sortKey)
            prefs.remove(includeArchivedKey)
        }
    }
}
```

Delegate được khai báo một lần ở top-level. Không tạo nhiều DataStore cho cùng file trong một process. Mẫu là single-process; ứng dụng nhiều process cần cấu hình tương ứng thay vì trộn single/multi-process cho cùng file. [Nguồn: DataStore — use correctly](https://developer.android.com/topic/libraries/architecture/datastore#use-datastore-correctly).

`edit` cung cấp read–modify–write được tuần tự hóa trong DataStore. Nếu tăng bộ đếm, đọc và ghi ngay trong cùng block edit; không lấy giá trị ngoài block rồi ghi lại sau. Ghi có thể ném lỗi, nên caller phải xử lý. Không gọi `runBlocking` trên main thread để biến DataStore thành getter đồng bộ.

Mẫu đọc dùng default khi thiếu key và khi có IOException, đồng thời ghi log lỗi đọc. Đây là fallback cho settings demo: không hiểu default là dữ liệu thực sự đã lưu. `catch` này không tự tạo cơ chế retry đọc vô hạn; app có yêu cầu retry hoặc hiển thị lỗi cần thêm state/retry riêng. Không nuốt CancellationException hay biến mọi lỗi thành preferences rỗng.

### 4.2 Migration từ SharedPreferences

Trình tự thực hành:

1. Chạy ví dụ LegacySettings và lưu TITLE_ASC, includeArchived=true.
2. Chuyển sang SettingsRepository với SharedPreferencesMigration như trên.
3. Collect DataStore; migration hoàn tất trước khi dữ liệu sẵn sàng cho reader.
4. Kiểm tra giá trị cũ được giữ, cả trường hợp chưa từng có file cũ.
5. Ngừng ghi SharedPreferences khi đã chuyển sang DataStore; tránh hai nguồn settings khác nhau.

Migration mặc định phù hợp các key/kiểu đã tương thích. Nếu rename key hoặc chuyển kiểu, dùng migration tùy chỉnh và test dữ liệu cũ. Migration DataStore và migration schema Room là hai cơ chế riêng. [Nguồn: DataStore guide](https://developer.android.com/topic/libraries/architecture/datastore).

## 5. Room: Entity, DAO và Database

### 5.1 Chuẩn bị dependency và code generation

Ví dụ hướng đến các API `androidx.room` trong Room 2.x, có `@Upsert` từ 2.5 trở lên, cùng Paging 3. Chọn phiên bản tương thích theo project; không sao chép version bất kỳ từ tài liệu khác.

| Artifact/cấu hình | Vai trò |
| --- | --- |
| `androidx.room:room-runtime` | Runtime Room |
| `androidx.room:room-compiler` qua KSP | Sinh implementation Database/DAO và kiểm tra query |
| `androidx.room:room-ktx` nếu cần theo version đang dùng | Tích hợp coroutine/Flow; một số version đã gộp API vào runtime |
| `androidx.room:room-paging` | DAO trả PagingSource |
| `androidx.datastore:datastore-preferences` | Preferences DataStore |
| `androidx.paging:paging-runtime` | PagingDataAdapter và Paging runtime cho Views |
| Lifecycle ViewModel KTX, Runtime KTX, LiveData KTX | viewModelScope, lifecycle collection, LiveData adapters |
| Fragment KTX, RecyclerView, AppCompat, coroutines Android | Host và UI Kotlin |
| `androidx.room:room-testing` ở androidTest | Test database/migration |

Bật KSP theo toolchain của dự án; đặt room-compiler trong `ksp(...)`, không chỉ implementation. Cấu hình export schema bằng Room Gradle plugin/schemaDirectory hoặc KSP arguments phù hợp version; commit schema JSON để review và test migration. [Nguồn: Room setup](https://developer.android.com/training/data-storage/room), [Upsert API](https://developer.android.com/reference/androidx/room/Upsert).

### 5.2 Entity và model kết quả thống kê

`NoteEntity.kt`:

```kotlin
package com.example.storagelesson

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["category", "createdAt", "id"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false
)

data class CategoryCount(
    val category: String,
    val total: Long,
    val latestCreatedAt: Long
)
```

`createdAt` dùng epoch milliseconds, category trong demo là một chuỗi không null. Primary key là identity của ghi chú; vị trí trên RecyclerView không phải ID. Dữ liệu có thể trùng title/time nhưng không được trùng ID.

`@ColumnInfo(defaultValue="0")` là default phía SQL/schema; `= false` là default phía Kotlin. Hai loại default có vai trò khác nhau, đặc biệt khi migration hoặc INSERT chỉ ghi một phần cột.

Index trên là điểm khởi đầu cho truy vấn theo category; không đảm bảo mọi query nhanh. Kiểm tra query plan và dataset thật trước khi thêm nhiều index vì index cũng làm tăng chi phí ghi/storage.

### 5.3 DAO: CRUD và observable query

`NotesDao.kt`:

```kotlin
package com.example.storagelesson

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity): Long

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun findById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY createdAt DESC, id ASC")
    fun observeNewestLiveData(): LiveData<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE (:includeArchived = 1 OR isArchived = 0)
          AND (:category IS NULL OR category = :category)
          AND title LIKE :pattern ESCAPE '\'
        ORDER BY
          CASE WHEN :sort = 'TITLE_ASC' THEN title COLLATE NOCASE END ASC,
          CASE WHEN :sort = 'NEWEST_FIRST' THEN createdAt END DESC,
          CASE WHEN :sort = 'OLDEST_FIRST' THEN createdAt END ASC,
          id ASC
    """)
    fun observeFiltered(
        pattern: String,
        category: String?,
        sort: String,
        includeArchived: Boolean
    ): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE (:includeArchived = 1 OR isArchived = 0)
          AND (:category IS NULL OR category = :category)
          AND title LIKE :pattern ESCAPE '\'
        ORDER BY
          CASE WHEN :sort = 'TITLE_ASC' THEN title COLLATE NOCASE END ASC,
          CASE WHEN :sort = 'NEWEST_FIRST' THEN createdAt END DESC,
          CASE WHEN :sort = 'OLDEST_FIRST' THEN createdAt END ASC,
          id ASC
    """)
    fun pagingSource(
        pattern: String,
        category: String?,
        sort: String,
        includeArchived: Boolean
    ): PagingSource<Int, NoteEntity>

    @Query("""
        SELECT category, COUNT(*) AS total, MAX(createdAt) AS latestCreatedAt
        FROM notes
        WHERE (:includeArchived = 1 OR isArchived = 0)
        GROUP BY category
        HAVING COUNT(*) >= :minimumCount
        ORDER BY total DESC, category COLLATE NOCASE ASC, category ASC
    """)
    fun observeCategoryCounts(
        includeArchived: Boolean,
        minimumCount: Int
    ): Flow<List<CategoryCount>>

    @Query("""
        SELECT * FROM notes WHERE isArchived = 0
        ORDER BY createdAt DESC, id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun loadOffsetPage(limit: Int, offset: Int): List<NoteEntity>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
          AND (createdAt < :lastTime OR (createdAt = :lastTime AND id > :lastId))
        ORDER BY createdAt DESC, id ASC
        LIMIT :limit
    """)
    suspend fun loadAfter(lastTime: Long, lastId: Long, limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id IN (:ids) ORDER BY id ASC")
    suspend fun findByIds(ids: List<Long>): List<NoteEntity>
}
```

`@Query` được Room kiểm tra khi code generation/build, gồm tên bảng/cột và ánh xạ kiểu trả về. `setArchived()`/`deleteById()` trả số dòng tác động; 0 không phải “đã sửa/xóa thành công một dòng”. `findById()` trả nullable vì ID có thể không tồn tại.

`@Upsert` dựa trên identity/primary key: entity có ID 0 và autoGenerate thường là bản ghi mới, không phải “cùng title thì tự update”. Tránh dùng REPLACE như cách update mặc định: ngữ nghĩa replace có thể ảnh hưởng identity/quan hệ/trigger khác với cập nhật. [Nguồn: Room DAO guide](https://developer.android.com/training/data-storage/room/accessing-data), [Upsert API](https://developer.android.com/reference/androidx/room/Upsert).

### 5.4 Database singleton và migration v1→v2

Giả sử v1 đã có `id`, `title`, `category`, `createdAt` và index như Entity trên, nhưng chưa có `isArchived`. V2 thêm cột này với default 0. App cài mới dùng schema v2; app có dữ liệu v1 dùng migration.

`NotesDatabase.kt`:

```kotlin
package com.example.storagelesson

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class], version = 2, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var instance: NotesDatabase? = null

        fun get(context: Context): NotesDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java,
                "lesson_notes.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
```

Không dùng `allowMainThreadQueries()` để né lỗi; DAO suspend/observable đã hỗ trợ truy cập bất đồng bộ. Không đóng singleton DB khi chỉ một Fragment bị hủy; vòng đời DB khác vòng đời View. Dependency injection có thể thay singleton thủ công trong project thực tế.

Thay Entity cần tăng version và tạo đường migration thích hợp. `fallbackToDestructiveMigration()` có thể xóa dữ liệu; không dùng để “chữa migration” của ghi chú người dùng. AutoMigration phù hợp một số thay đổi đơn giản, vẫn cần schema đã export và kiểm thử. [Nguồn: Migration API](https://developer.android.com/reference/androidx/room/migration/Migration).

## 6. Custom query: lọc, tìm kiếm, projection và query động

### 6.1 Bind parameter là giá trị, không phải cấu trúc SQL

`WHERE id = :id` nhận giá trị ID. Không ghép trực tiếp input người dùng vào chuỗi SQL. Với `IN (:ids)`, Room xử lý danh sách tham số; danh sách rất lớn cần cân nhắc giới hạn bind của SQLite và chia batch phù hợp.

`ORDER BY :column` không biến giá trị String thành tên cột; `:direction` cũng không thay được keyword ASC/DESC. Với vài tùy chọn sort, dùng các query riêng hoặc CASE như DAO mẫu. Khi cần cấu trúc động thực sự, dùng RawQuery với whitelist cấu trúc và bind giá trị.

### 6.2 LIKE: tìm chuỗi con và wildcard

SQL mẫu dùng `LIKE :pattern ESCAPE '\'`, gọi bằng `containsPattern(keyword)`:

| Input | Ý nghĩa cần có |
| --- | --- |
| `Room` | Tìm title chứa chuỗi Room |
| `%` | Tìm dấu % thật, không chọn mọi dòng |
| `_` | Tìm dấu gạch dưới thật, không khớp một ký tự bất kỳ |
| Dấu `\` | Escape chính ký tự escape trước khi escape % và _ |
| Rỗng hoặc chỉ khoảng trắng | Pattern %%: không hạn chế title trong demo |

Bind parameter ngăn input trở thành cú pháp SQL nhưng không tự loại bỏ ngữ nghĩa wildcard của LIKE. Hàm escape và mệnh đề ESCAPE cần đi cùng nhau. Tìm `%keyword%` thường không tận dụng B-tree index thông thường theo cách tìm prefix; bài toán tìm kiếm lớn nên đánh giá FTS và yêu cầu tìm kiếm tiếng Việt. [Nguồn: SQLite expressions — LIKE](https://www.sqlite.org/lang_expr.html).

### 6.3 Chỉ lấy cột cần thiết: projection và JOIN

Snippet dưới được thêm vào NotesDao, đồng thời thêm model ở file riêng hoặc cạnh Entity:

```kotlin
data class NotePreview(val id: Long, val title: String)

// Trong NotesDao:
@Query("SELECT id, title FROM notes WHERE category = :category ORDER BY id ASC")
suspend fun previews(category: String): List<NotePreview>
```

Nếu category có metadata/ID riêng, tách Entity Category và Note có categoryId, foreign key và index. Khi query JOIN, dùng alias cho tên cột trùng và trả DTO phù hợp; `@Relation` là cách đọc quan hệ, không tự tạo foreign key. Query có nhiều lần đọc để dựng quan hệ nên dùng `@Transaction` khi cần snapshot nhất quán. [Nguồn: Room relationships](https://developer.android.com/training/data-storage/room/relationships), [DAO query guide](https://developer.android.com/training/data-storage/room/accessing-data).

### 6.4 RawQuery có kiểm soát

Mở rộng tùy chọn dưới dành cho Room Android dùng SupportSQLiteQuery. Room có cả API RoomRawQuery ở các cấu hình mới; chọn loại hỗ trợ bởi version/driver đang dùng, không trộn chữ ký tùy ý.

Thêm vào NotesDao với các import `RawQuery` và `SupportSQLiteQuery`:

```kotlin
@RawQuery(observedEntities = [NoteEntity::class])
fun observeRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<List<NoteEntity>>
```

`NoteQueries.kt`:

```kotlin
package com.example.storagelesson

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

fun buildNoteQuery(keyword: String, sort: SortOrder): SupportSQLiteQuery {
    val orderClause = when (sort) {
        SortOrder.NEWEST_FIRST -> "createdAt DESC, id ASC"
        SortOrder.OLDEST_FIRST -> "createdAt ASC, id ASC"
        SortOrder.TITLE_ASC -> "title COLLATE NOCASE ASC, id ASC"
    }
    val sql = "SELECT * FROM notes WHERE isArchived = 0 " +
        "AND title LIKE ? ESCAPE '\\' ORDER BY " + orderClause
    return SimpleSQLiteQuery(sql, arrayOf<Any>(containsPattern(keyword)))
}
```

orderClause chỉ đến từ enum của app; keyword truyền qua bind argument. Không nhận nguyên đoạn SQL/order clause từ input. RawQuery không có cùng mức kiểm tra SQL compile-time như Query; test là bắt buộc khi chọn hướng này. `observedEntities` khai báo bảng cần theo dõi cho query observable. [Nguồn: RawQuery API](https://developer.android.com/reference/androidx/room/RawQuery).

## 7. Quan sát dữ liệu bằng LiveData và Flow

### 7.1 Lấy một lần và observable khác nhau

| Chữ ký DAO | Dữ liệu nhận được | Trường hợp dùng |
| --- | --- | --- |
| `suspend fun ...: List<NoteEntity>` | Một snapshot khi query chạy | Export, xử lý một lần, trang thủ công |
| `fun ...: LiveData<List<NoteEntity>>` | Kết quả cập nhật khi bảng đổi và observer hoạt động | UI Views theo LiveData |
| `fun ...: Flow<List<NoteEntity>>` | Stream query khi được collect | Kết hợp filter/settings bằng coroutine Flow |
| `fun ...: PagingSource<Int, NoteEntity>` | Nguồn trang có invalidation | Danh sách lớn với Paging |

Một hàm suspend không tự thành observer. Query Flow thường không khai báo suspend: nó trả stream, còn Room thực hiện query khi collect. Một Flow nullable entity phù hợp khi item có thể bị xóa; list rỗng là kết quả hợp lệ của query list.

Room observable query có thể chạy lại khi một dòng bất kỳ trong bảng liên quan đổi, kể cả dòng không nằm trong kết quả filter. `distinctUntilChanged()` giảm emission bằng nhau xuống UI, không ngăn DB chạy lại query. LiveData cũng có cơ chế distinct tương ứng. [Nguồn: Room asynchronous queries](https://developer.android.com/training/data-storage/room/async-queries).

### 7.2 LiveData trong MVVM

Trong ViewModel, expose DAO qua repository; trên Fragment observe với viewLifecycleOwner:

```kotlin
// Trong NotesViewModel ở phần 11:
val newestLiveData = notes.observeNewestLiveData()

// Trong Fragment demo riêng cho LiveData, đã có listAdapter:
viewModel.newestLiveData.observe(viewLifecycleOwner) { rows ->
    listAdapter.submitList(rows)
}
```

`listAdapter` ở đây là ListAdapter thông thường đã học ở bài RecyclerView. Không gọi `PagingDataAdapter.submitData(rows)` với List; hai adapter nhận dữ liệu khác nhau. Query LiveData này cố định “mới nhất, chưa lưu trữ”, không tự chịu filter của màn hình Paging.

Nếu code UI cần LiveData nhưng repository cung cấp Flow, có thể `flow.asLiveData()` ở ViewModel với LiveData KTX. Không dùng observeForever trong Fragment để né lifecycle.

### 7.3 Flow trong Fragment

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.filteredRows.collect { rows ->
            // ListAdapter.submitList(rows) trong màn hình list nhỏ riêng.
        }
    }
}
```

Snippet cần import lifecycleScope, repeatOnLifecycle, Lifecycle và launch. Collect nhiều Flow cùng lúc bằng các `launch` con trong block repeatOnLifecycle; đặt hai lệnh collect tuần tự sẽ làm collect thứ hai chờ mãi nếu stream thứ nhất không kết thúc.

`flatMapLatest` phù hợp khi filter đổi: hủy stream query cũ và chuyển sang filter mới. `stateIn` giữ state phục vụ UI, không phải cơ chế lưu disk. ViewModel còn sống qua configuration change không đồng nghĩa mọi field sống qua process death; dùng SavedStateHandle/persistent storage theo loại state.

## 8. Sort: SQL phải sắp xếp đúng toàn bộ tập dữ liệu

Ví dụ NEWEST_FIRST của DAO: `ORDER BY createdAt DESC, id ASC`. Cột id là tie-breaker để các dòng trùng timestamp có thứ tự xác định. Điều này quan trọng cho pagination và UI diff.

Các nguyên tắc:

- Sort trước LIMIT; lấy một trang rồi sort trong Kotlin chỉ sort trang đã tải.
- Không `.sortedBy` vài trang đã tải rồi xem đó là thứ tự toàn DB.
- Thay sort tạo query/PagingSource mới; reset/khôi phục vị trí theo yêu cầu UI.
- Không suy ra thứ tự chèn từ SELECT không có ORDER BY.
- CASE ORDER BY linh hoạt nhưng có thể làm index khó được dùng hiệu quả; query riêng cho từng sort có thể tốt hơn khi dataset lớn.

TITLE_ASC trong demo dùng SQLite NOCASE, chủ yếu hỗ trợ case folding ASCII mặc định. Đây không phải bảo đảm sort đúng thứ tự tiếng Việt/ngôn ngữ của thiết bị. Nếu cần sort/search theo ngôn ngữ, thiết kế khóa sắp xếp/collation phù hợp và test các tên có dấu. [Nguồn: SQLite SELECT — ORDER BY](https://sqlite.org/lang_select.html), [SQLite expressions — collating operators](https://www.sqlite.org/lang_expr.html).

## 9. Group: thống kê SQL và nhóm item UI là hai nhu cầu khác nhau

### 9.1 GROUP BY + aggregate + HAVING

Query observeCategoryCounts trả **một dòng cho mỗi category**, gồm count và timestamp lớn nhất. `WHERE` lọc dòng trước khi nhóm; `HAVING` lọc nhóm sau khi aggregate. Nếu không có dòng phù hợp, kết quả là list rỗng, không phải một nhóm total=0 cho từng category chưa có note.

Ví dụ dữ liệu kiểm chứng (timestamp là số nhỏ để dễ đọc; app thực tế dùng epoch milliseconds):

| id | title | category | createdAt | isArchived |
| ---: | --- | --- | ---: | ---: |
| 1 | Alpha | Android | 100 | 0 |
| 2 | beta | Android | 200 | 0 |
| 3 | Gamma | Kotlin | 200 | 0 |
| 4 | 100% done | Kotlin | 150 | 0 |
| 5 | under_score | Android | 300 | 1 |
| 6 | Alpha | Android | 200 | 0 |

Khi includeArchived=false, minimumCount=2:

| category | total | latestCreatedAt |
| --- | ---: | ---: |
| Android | 3 | 200 |
| Kotlin | 2 | 200 |

Nếu includeArchived=true, Android có total=4 và latestCreatedAt=300. GROUP BY không trả toàn bộ NoteEntity của nhóm; model CategoryCount phản ánh đúng projection. Tránh `SELECT * ... GROUP BY category`: các cột không aggregate có thể không thuộc dòng mà bạn tưởng. [Nguồn: SQLite SELECT — aggregation](https://sqlite.org/lang_select.html).

### 9.2 Group để hiển thị header trên RecyclerView

Màn hình có header Android rồi các note Android cần các dòng chi tiết, không chỉ COUNT. Với dataset nhỏ, lấy list đã sort và groupBy/map thành UI model. Với paging lớn, sắp xếp theo khóa group trong SQL rồi thêm separator giữa các nhóm.

Ví dụ nếu grouping chính xác theo chuỗi category, dùng thứ tự `category COLLATE NOCASE ASC, category ASC, createdAt DESC, id ASC`. Tie-breaker category dạng binary giúp các tên chỉ khác hoa/thường không bị trộn lẫn trong cùng thứ tự case-insensitive; hoặc chuẩn hóa category theo ID từ đầu.

Snippet mở rộng của một Flow<PagingData<NoteEntity>>:

```kotlin
sealed interface NoteRow {
    data class Item(val note: NoteEntity) : NoteRow
    data class Header(val category: String) : NoteRow
}

// source là Flow<PagingData<NoteEntity>> đã sort theo category.
val groupedRows = source.map { pagingData ->
    pagingData.map<NoteEntity, NoteRow> { NoteRow.Item(it) }
        .insertSeparators<NoteRow, NoteRow> { before, after ->
            val next = (after as? NoteRow.Item)?.note ?: return@insertSeparators null
            val previous = (before as? NoteRow.Item)?.note
            if (previous?.category != next.category) NoteRow.Header(next.category) else null
        }
}.cachedIn(viewModelScope)
```

Cần import Paging `map`, `insertSeparators`, `cachedIn` và Flow `map`. Đây là mở rộng: adapter phải hỗ trợ Item/Header bằng nhiều ViewType; NotesPagingAdapter phần 12 chỉ hiển thị NoteEntity. Không group độc lập từng page vì một group có thể kéo dài qua ranh giới page. Không tính tổng category từ số item đã tải; query GROUP BY trên DB cho thống kê toàn bộ. [Nguồn: Paging transformations cho Views](https://developer.android.com/topic/libraries/architecture/views/paging/v3-transform-views).

## 10. Phân trang: OFFSET, keyset và Paging 3

### 10.1 LIMIT/OFFSET thủ công

Với pageIndex bắt đầu từ 0, offset = pageIndex × pageSize. Page size phải dương, offset không âm; nếu dùng Int cần kiểm tra overflow với input không kiểm soát.

```kotlin
// Chạy trong coroutine; dao là NotesDao đã được cung cấp.
val pageSize = 2
val pageIndex = 1
require(pageSize > 0 && pageIndex >= 0)
val rows = dao.loadOffsetPage(limit = pageSize, offset = pageIndex * pageSize)
```

Với dữ liệu mẫu, thứ tự chưa archive mới nhất là [2, 3, 6, 4, 1]. Trang size=2 lần lượt [2,3], [6,4], [1]. Không có ORDER BY ổn định thì kết quả giữa các lần tải không được bảo đảm.

OFFSET lớn có chi phí bỏ qua các dòng trước đó. Khi chèn/xóa trước ranh giới giữa hai lần tải, offset thay đổi có thể gây trùng/bỏ sót item; app phải có chiến lược refresh/snapshot và chống trùng khi tự nối list.

### 10.2 Keyset/cursor

DAO loadAfter dùng cả `createdAt` và `id` làm cursor vì order là time DESC nhưng ID ASC:

```text
Đi sau dòng (lastTime, lastId) nếu:
createdAt < lastTime
HOẶC createdAt = lastTime và id > lastId
```

Trang đầu có thể lấy bằng loadOffsetPage(limit, 0); trang sau dùng cursor từ dòng cuối. Ví dụ cursor (200,3) trả [6,4] với limit2. Nếu chỉ dùng time, dòng 6 trùng timestamp có thể bị bỏ sót.

Keyset thường thuận lợi cho forward scrolling nhưng không tự xử lý mọi cập nhật/reorder, tải ngược hoặc nhảy trang số N. Thay sort/filter phải đổi/reset cursor. Index phù hợp giúp query nhưng không có một index tối ưu cho mọi tổ hợp.

### 10.3 Paging 3 + Room

```text
Filter/Preferences → Pager → DAO tạo PagingSource mới
                       │
                       ▼
                Flow<PagingData<NoteEntity>>
                       │ cachedIn(viewModelScope)
                       ▼
               PagingDataAdapter.submitData
```

DAO pagingSource không tự thêm LIMIT/OFFSET thủ công; Room tạo phần tải trang cho PagingSource. Factory phải tạo một instance mới mỗi lần Pager yêu cầu, không cache một PagingSource đã invalidated. Bảng đổi khiến source liên quan invalidated và thế hệ dữ liệu mới được tải khi stream đang hoạt động.

PagingConfig.pageSize là kích thước mong muốn cho tải trang, không cam kết mọi lần load trả đúng pageSize: initialLoadSize/prefetch và nguồn dữ liệu có thể khác. `cachedIn(viewModelScope)` giúp tái sử dụng thế hệ PagingData trong vòng đời ViewModel; không phải lưu disk. Dữ liệu bền vững ở đây đến từ Room. [Nguồn: Paging overview](https://developer.android.com/topic/libraries/architecture/paging/v3-overview), [PagingDataAdapter API](https://developer.android.com/reference/androidx/paging/PagingDataAdapter).

## 11. Repository và ViewModel cho màn hình thực hành

### 11.1 Repository

`NotesRepository.kt`:

```kotlin
package com.example.storagelesson

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class NotesRepository(private val database: NotesDatabase) {
    private val dao = database.notesDao()

    fun observeNewestLiveData() = dao.observeNewestLiveData()

    fun observe(filter: NoteFilter): Flow<List<NoteEntity>> = dao.observeFiltered(
        filter.pattern, filter.category, filter.sort.name, filter.includeArchived
    ).distinctUntilChanged()

    fun pages(filter: NoteFilter): Flow<PagingData<NoteEntity>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            initialLoadSize = 40,
            prefetchDistance = 5,
            enablePlaceholders = false
        ),
        pagingSourceFactory = {
            dao.pagingSource(
                filter.pattern, filter.category, filter.sort.name, filter.includeArchived
            )
        }
    ).flow

    fun counts(includeArchived: Boolean): Flow<List<CategoryCount>> =
        dao.observeCategoryCounts(includeArchived, minimumCount = 1)
            .distinctUntilChanged()

    suspend fun add(title: String, category: String): Long {
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        require(cleanTitle.isNotEmpty() && cleanCategory.isNotEmpty())
        return dao.insert(
            NoteEntity(
                title = cleanTitle,
                category = cleanCategory,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun archive(id: Long) {
        check(dao.setArchived(id, true) == 1) { "Note no longer exists" }
    }

    suspend fun importAndArchive(records: List<NoteEntity>, archiveId: Long) {
        database.withTransaction {
            dao.upsertAll(records)
            check(dao.setArchived(archiveId, true) == 1)
        }
    }
}
```

importAndArchive minh họa một transaction gồm nhiều thao tác: nếu check thất bại, ghi trước đó phải rollback. Không đặt network request hoặc chờ người dùng trong transaction. `@Upsert` một collection đã có ngữ nghĩa transaction cho thao tác đó; withTransaction dùng khi cần gộp thêm thao tác khác.

### 11.2 ViewModel

`NotesViewModel.kt`:

```kotlin
package com.example.storagelesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(
    private val notes: NotesRepository,
    private val preferences: SettingsRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val search = savedState.getStateFlow(SEARCH, "")
    private val category = MutableStateFlow<String?>(null)
    private val _writeError = MutableStateFlow(false)
    val writeError: StateFlow<Boolean> = _writeError.asStateFlow()

    val settings = preferences.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings()
    )

    private val filters = combine(
        search.debounce(250), category, preferences.settings
    ) { keyword, categoryValue, userSettings ->
        NoteFilter(
            containsPattern(keyword), categoryValue,
            userSettings.sort, userSettings.includeArchived
        )
    }.distinctUntilChanged()

    val pagedNotes = filters.flatMapLatest(notes::pages).cachedIn(viewModelScope)

    // Cho màn hình list nhỏ riêng; không collect nếu đang dùng Paging cho list lớn.
    val filteredRows = filters.flatMapLatest(notes::observe)

    // Cho ví dụ LiveData riêng ở phần 7; query này có filter cố định.
    val newestLiveData = notes.observeNewestLiveData()

    // Thống kê toàn DB theo includeArchived, không áp dụng keyword/category của list.
    val categoryCounts = preferences.settings
        .map { it.includeArchived }
        .distinctUntilChanged()
        .flatMapLatest(notes::counts)

    fun setSearch(value: String) { savedState[SEARCH] = value }
    fun currentSearch(): String = search.value
    fun setCategory(value: String?) { category.value = value }

    fun setSort(value: SortOrder) = write { preferences.setSort(value) }
    fun setIncludeArchived(value: Boolean) = write { preferences.setIncludeArchived(value) }
    fun add(title: String, category: String) = write { notes.add(title, category); Unit }
    fun archive(id: Long) = write { notes.archive(id) }
    fun resetSettings() = write { preferences.reset() }

    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            _writeError.value = false
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Demo giữ cờ lỗi cho UI; app thật nên phân loại và ghi log phù hợp.
                _writeError.value = true
            }
        }
    }

    companion object { private const val SEARCH = "search" }
}
```

Không hủy rồi tạo mới ViewModel mỗi lần đổi sort. Filter đổi tạo Pager/source mới qua flatMapLatest. `cachedIn` đặt sau các transform của Flow paging để UI có thể collect lại khi View được tạo lại. Chỉ thu thập filteredRows hoặc newestLiveData ở demo riêng; collect full list bên cạnh Paging sẽ làm mất mục đích tải theo trang.

Search nằm trong SavedStateHandle để hỗ trợ recreation; category hiện chỉ là state trong memory, thêm SavedStateHandle nếu cần restore. Sort và includeArchived nằm trên disk. Cờ writeError là state hiển thị lỗi đơn giản, không phải event Toast dùng một lần và không thay thế xử lý query/load error của Paging.

### 11.3 Factory có SavedStateHandle

`NotesViewModelFactory.kt`:

```kotlin
package com.example.storagelesson

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

fun notesViewModelFactory(
    notes: NotesRepository,
    settings: SettingsRepository
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        NotesViewModel(notes, settings, createSavedStateHandle())
    }
}
```

Factory dùng các API Lifecycle hiện đại; Fragment KTX cung cấp CreationExtras/SavedState owner khi dùng `by viewModels`. Project đã có DI có thể thay phần wiring nhưng vẫn cần cung cấp SavedStateHandle đúng owner.

## 12. UI thực hành với PagingDataAdapter

Màn hình có tìm kiếm, thêm note, đổi sort, hiện archive, thống kê category và danh sách. Bấm một note để archive nó; đây là tương tác demo, sản phẩm nên có hành động rõ ràng phù hợp UX. Category thêm mới cố định Android để rút gọn UI; bài tập mở rộng yêu cầu nhập/chọn category.

### 12.1 Resource

`res/values/strings.xml`:

```xml
<resources>
    <string name="search_hint">Tìm theo tiêu đề</string>
    <string name="note_title_hint">Tiêu đề ghi chú mới</string>
    <string name="add_note">Thêm</string>
    <string name="include_archived">Hiện ghi chú đã lưu trữ</string>
    <string name="sort_newest">Mới nhất</string>
    <string name="sort_oldest">Cũ nhất</string>
    <string name="sort_title">Tiêu đề A–Z</string>
    <string name="sort_action">Sắp xếp: %1$s</string>
    <string name="demo_category">Android</string>
    <string name="group_item">%1$s: %2$d</string>
    <string name="note_item">%1$s — %2$s%3$s</string>
    <string name="archived_suffix"> (đã lưu trữ)</string>
    <string name="archive_action_description">%1$s. Chạm để lưu trữ ghi chú.</string>
    <string name="empty_notes">Chưa có ghi chú phù hợp</string>
    <string name="write_error">Không thể lưu thay đổi. Kiểm tra dữ liệu và thử lại.</string>
    <string name="load_error">Không thể tải ghi chú</string>
    <string name="retry_load">Thử tải lại</string>
</resources>
```

`res/layout/fragment_notes_storage.xml`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/searchInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:hint="@string/search_hint" />

    <EditText
        android:id="@+id/newTitleInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textCapSentences"
        android:hint="@string/note_title_hint" />

    <Button
        android:id="@+id/addNoteButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/add_note" />

    <Button
        android:id="@+id/sortButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <androidx.appcompat.widget.SwitchCompat
        android:id="@+id/includeArchivedSwitch"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/include_archived" />

    <TextView
        android:id="@+id/categorySummary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <TextView
        android:id="@+id/writeErrorText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/write_error"
        android:visibility="gone" />

    <TextView
        android:id="@+id/loadErrorText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/load_error"
        android:visibility="gone" />

    <Button
        android:id="@+id/retryButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/retry_load"
        android:visibility="gone" />

    <ProgressBar
        android:id="@+id/notesLoading"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

    <TextView
        android:id="@+id/emptyNotesText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/empty_notes"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/notesList"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
</LinearLayout>
```

`res/layout/item_storage_note.xml`:

```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/noteLabel"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="48dp"
    android:gravity="center_vertical"
    android:padding="12dp"
    android:clickable="true"
    android:focusable="true" />
```

Với nhiều control và font lớn, có thể cần thiết kế toolbar/filter panel gọn hơn; không bọc list Paging lớn trong NestedScrollView để giải quyết màn hình chật. Áp dụng system insets theo quy ước app.

### 12.2 Adapter

`NotesPagingAdapter.kt`:

```kotlin
package com.example.storagelesson

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class NotesPagingAdapter(
    private val onArchive: (Long) -> Unit
) : PagingDataAdapter<NoteEntity, NotesPagingAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val label = view.findViewById<TextView>(R.id.noteLabel)

        fun bind(note: NoteEntity?, onArchive: (Long) -> Unit) {
            val context = itemView.context
            label.text = if (note == null) "" else context.getString(
                R.string.note_item, note.title, note.category,
                if (note.isArchived) context.getString(R.string.archived_suffix) else ""
            )
            itemView.isEnabled = note != null && !note.isArchived
            itemView.contentDescription = when {
                note == null -> null
                note.isArchived -> label.text
                else -> context.getString(R.string.archive_action_description, label.text)
            }
            itemView.setOnClickListener {
                if (note != null && !note.isArchived) onArchive(note.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage_note, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), onArchive)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NoteEntity>() {
            override fun areItemsTheSame(old: NoteEntity, new: NoteEntity) = old.id == new.id
            override fun areContentsTheSame(old: NoteEntity, new: NoteEntity) = old == new
        }
    }
}
```

Click truyền ID entity, không truyền position cũ. Bind cả text, enable, description và listener để không giữ state của item trước. `getItem()` là cách truy cập item và báo nhu cầu tải cho Paging. Không gọi setHasStableIds trên PagingDataAdapter; identity dùng DiffUtil theo cơ chế adapter. [Nguồn: PagingDataAdapter API](https://developer.android.com/reference/androidx/paging/PagingDataAdapter).

### 12.3 Fragment: collect nhiều stream và load state

`NotesStorageFragment.kt`:

```kotlin
package com.example.storagelesson

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesStorageFragment : Fragment(R.layout.fragment_notes_storage) {
    private val viewModel: NotesViewModel by viewModels {
        val context = requireContext().applicationContext
        notesViewModelFactory(
            NotesRepository(NotesDatabase.get(context)), SettingsRepository(context)
        )
    }
    private var recycler: RecyclerView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = view.findViewById<RecyclerView>(R.id.notesList)
        val search = view.findViewById<EditText>(R.id.searchInput)
        val newTitle = view.findViewById<EditText>(R.id.newTitleInput)
        val add = view.findViewById<Button>(R.id.addNoteButton)
        val sort = view.findViewById<Button>(R.id.sortButton)
        val include = view.findViewById<SwitchCompat>(R.id.includeArchivedSwitch)
        val summary = view.findViewById<TextView>(R.id.categorySummary)
        val writeError = view.findViewById<TextView>(R.id.writeErrorText)
        val loadError = view.findViewById<TextView>(R.id.loadErrorText)
        val retry = view.findViewById<Button>(R.id.retryButton)
        val loading = view.findViewById<ProgressBar>(R.id.notesLoading)
        val empty = view.findViewById<TextView>(R.id.emptyNotesText)
        val adapter = NotesPagingAdapter(viewModel::archive)
        var currentSort = SortOrder.NEWEST_FIRST
        var renderingSettings = false

        recycler = list
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        search.setText(viewModel.currentSearch())
        search.doAfterTextChanged { viewModel.setSearch(it?.toString().orEmpty()) }

        add.setOnClickListener {
            val title = newTitle.text.toString()
            if (title.isNotBlank()) {
                viewModel.add(title, getString(R.string.demo_category))
                // Giữ input để người dùng có thể thử lại nếu ghi thất bại.
            }
        }
        sort.setOnClickListener {
            val next = when (currentSort) {
                SortOrder.NEWEST_FIRST -> SortOrder.OLDEST_FIRST
                SortOrder.OLDEST_FIRST -> SortOrder.TITLE_ASC
                SortOrder.TITLE_ASC -> SortOrder.NEWEST_FIRST
            }
            viewModel.setSort(next)
        }
        include.setOnCheckedChangeListener { _, checked ->
            if (!renderingSettings) viewModel.setIncludeArchived(checked)
        }
        retry.setOnClickListener { adapter.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pagedNotes.collectLatest { adapter.submitData(it) }
                }
                launch {
                    viewModel.settings.collect { settings ->
                        currentSort = settings.sort
                        val label = when (settings.sort) {
                            SortOrder.NEWEST_FIRST -> R.string.sort_newest
                            SortOrder.OLDEST_FIRST -> R.string.sort_oldest
                            SortOrder.TITLE_ASC -> R.string.sort_title
                        }
                        sort.text = getString(R.string.sort_action, getString(label))
                        renderingSettings = true
                        include.isChecked = settings.includeArchived
                        renderingSettings = false
                    }
                }
                launch {
                    viewModel.categoryCounts.collect { counts ->
                        summary.text = counts.joinToString("; ") {
                            getString(R.string.group_item, it.category, it.total)
                        }
                    }
                }
                launch {
                    viewModel.writeError.collect { failed ->
                        writeError.visibility = if (failed) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    adapter.loadStateFlow.collect { state ->
                        val busy = state.refresh is LoadState.Loading
                        val failed = state.refresh is LoadState.Error ||
                            state.append is LoadState.Error || state.prepend is LoadState.Error
                        loading.visibility = if (busy) View.VISIBLE else View.GONE
                        loadError.visibility = if (failed) View.VISIBLE else View.GONE
                        retry.visibility = if (failed) View.VISIBLE else View.GONE
                        val noRows = state.refresh is LoadState.NotLoading &&
                            !failed && adapter.itemCount == 0
                        empty.visibility = if (noRows) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        recycler?.adapter = null
        recycler = null
        super.onDestroyView()
    }
}
```

Host app chỉ cần hiển thị NotesStorageFragment bằng Navigation hoặc Fragment transaction đã học. Fragment lấy DB bằng applicationContext; ViewModel không giữ Activity/View. Collector bị hủy khi View bị hủy và khởi động lại theo lifecycle; adapter của View cũ được gỡ.

Đây là demo luồng storage, chưa có đầy đủ trạng thái “đang lưu”, validation chi tiết hoặc lỗi statistics. Mở rộng sản phẩm cần xử lý categoryCounts/query error, disable/chống submit trùng khi đang ghi và feedback thành công. Room Flow không tự biến exception thành UI error state. Paging load error đi qua LoadState; error khi ghi/đọc settings cần luồng xử lý riêng.

## 13. Dữ liệu, migration và hiệu năng cần nắm

### 13.1 Dữ liệu tồn tại qua tình huống nào?

| Tình huống | Preferences/Room | State memory trong ViewModel |
| --- | --- | --- |
| Xoay máy | Vẫn lưu; UI đọc/restore lại | Thường giữ nếu cùng ViewModelStore owner |
| Process bị hủy rồi task được phục hồi | Dữ liệu disk vẫn còn | Field memory mất; SavedStateHandle có thể restore state phù hợp |
| Mở lại app sau đóng phiên | Đọc dữ liệu persistent | Tạo ViewModel mới |
| Xóa app data/uninstall | Thường mất dữ liệu local tương ứng; còn phụ thuộc backup/restore | Mất |
| Nâng app đổi schema | Cần migration DB đúng | Tạo state theo phiên hiện tại |

Không xem thao tác quay lại Fragment là “đọc DB mới hoàn toàn”: việc query/observe phụ thuộc lifecycle, cache và scope đang hoạt động. UI nên render từ source of truth thay vì giữ một list mutable riêng không đồng bộ với DB.

### 13.2 Kiểm chứng migration

- Giữ schema v1 và v2 đúng thực tế; không tự dựng v1 khác với schema người dùng đã cài.
- Dùng MigrationTestHelper/Room testing để tạo DB v1, insert dữ liệu, chạy migration và validate schema v2.
- Assert ID/title/category/time giữ nguyên; cột isArchived của dòng cũ bằng false.
- Mở DB qua builder của app sau migrate và đọc bằng DAO.
- Test cài mới v2 và nâng từ mọi version còn hỗ trợ, không chỉ một đường migration.

SQL ALTER TABLE chạy được trên SQLite chưa chứng minh schema khớp Room; Room còn kiểm tra metadata/schema mong đợi. Mẫu migration trong bài chỉ đúng với giả định v1 đã nêu. [Nguồn: Migration API](https://developer.android.com/reference/androidx/room/migration/Migration).

### 13.3 Index và truy vấn

Với truy vấn quan trọng, xem `EXPLAIN QUERY PLAN` trong công cụ DB/SQLite phù hợp. Đánh giá đủ dữ liệu để thấy khác biệt; sáu dòng demo không phải benchmark.

- Projection giúp giảm dữ liệu lấy khi UI chỉ cần vài cột.
- B-tree index có thứ tự; thứ tự cột index phải phù hợp filter/order thường dùng.
- Nhiều optional filter OR và CASE ORDER BY có thể đổi query plan.
- Query GROUP BY/statistics có thể quét nhiều dữ liệu; tránh chạy lại quá thường xuyên nếu không cần.
- Cập nhật nhiều bản ghi theo batch/transaction phù hợp, tránh một action phát hàng nghìn lần ghi rời rạc.
- File lớn nên lưu bằng file API, không đưa toàn bộ bitmap vào prefs hoặc DB chỉ để tiện.

## 14. Bài tập và kiểm thử

### 14.1 Bài tập theo mức độ

**Bài 1 — Preferences:** lưu/đọc/xóa sort và includeArchived bằng SharedPreferences. Giải thích apply/commit, key missing và key sai kiểu. Chuyển sang DataStore và chứng minh migration giữ lựa chọn cũ.

**Bài 2 — Room CRUD:** triển khai Entity/DAO/Database/repository. Thêm, update title, archive và delete theo ID; phân biệt “không tìm thấy” với “list rỗng”. Thêm DAO update title trả số dòng, validate title ở repository.

**Bài 3 — Observable:** tạo màn hình ListAdapter nhỏ với LiveData, rồi màn hình tương tự với Flow. Thêm/sửa/xóa ở DB và kiểm tra UI tự đổi, không gọi reload thủ công. Kiểm tra collector khi host background, xoay máy và View bị hủy.

**Bài 4 — Query:** seed sáu dòng phần 9; kiểm tra search `%`, `_`, filter category, sort cả ba kiểu, GROUP BY và HAVING. Viết projection chỉ lấy id/title; giải thích vì sao ORDER BY :column sai mục đích.

**Bài 5 — Pagination:** chạy OFFSET size2 và keyset size2; insert một dòng mới trước khi tải trang tiếp để so sánh hành vi. Triển khai PagingDataAdapter và load state. Seed ít nhất vài trăm dòng để kiểm tra cuộn/tải thêm.

**Bài 6 — Group UI:** thêm header category bằng insertSeparators và nhiều ViewType. Một category có hơn một page; header không được xuất hiện lại sai tại ranh giới page. Thống kê count lấy từ query DB, không từ item đã tải.

**Bài 7 — Migration/transaction:** cài v1 có dữ liệu rồi nâng v2. Kiểm tra schema/dữ liệu. Gọi importAndArchive với ID không tồn tại; assert records đã upsert trong transaction không bị commit.

### 14.2 Kết quả SQL bắt buộc với seed mẫu

**Kiểm chứng trong workspace:** đã thực thi 11 annotation Query mẫu trên SQLite 3.53.1 và đối chiếu filter, ba kiểu sort, wildcard literal, GROUP BY/HAVING, OFFSET và keyset. SQL query động, ALTER TABLE giữ dữ liệu và rollback cũng đã được kiểm tra. SQLite của thiết bị Android có thể khác phiên bản; kết quả này chưa thay cho Room compiler, migration schema validation hoặc kiểm thử trên Android.

| Query/điều kiện | Kết quả mong đợi |
| --- | --- |
| NEWEST_FIRST, chưa archive | ID [2, 3, 6, 4, 1] |
| OLDEST_FIRST, chưa archive | ID [1, 4, 2, 3, 6] |
| TITLE_ASC, chưa archive | ID [4, 1, 6, 2, 3] với NOCASE mặc định |
| Category Android, chưa archive, newest | ID [2, 6, 1] |
| Search dấu % thật | ID [4] |
| Search dấu _ thật, include archive | ID [5] |
| GROUP BY chưa archive, min2 | Android=3, Kotlin=2 |
| GROUP BY chưa archive, min3 | Chỉ Android=3 |
| OFFSET limit2 offset2 | ID [6, 4] |
| Keyset sau (200,3), limit2 | ID [6, 4] |
| ID không tồn tại | findById=null; update/delete trả 0 |

### 14.3 Ma trận kiểm thử ứng dụng

| Thao tác | Kết quả cần thấy |
| --- | --- |
| Cài mới, chưa có settings/notes | Default settings đúng; empty state sau khi tải xong |
| Thêm note hợp lệ | Note xuất hiện theo sort; count category đổi |
| Title toàn khoảng trắng | Không ghi dữ liệu không hợp lệ |
| Bấm archive | Dòng biến mất nếu không hiện archive; count đổi tương ứng |
| Bật hiện archive | Dòng archive xuất hiện, action demo không archive lặp |
| Đổi sort rồi restart app | Lựa chọn lưu; toàn dataset theo thứ tự mới |
| Gõ tìm kiếm nhanh | Query mới thay query cũ; không chồng kết quả search cũ |
| Search %, _ hoặc dấu nháy | Kết quả theo literal; không đổi cấu trúc SQL |
| Hai dòng trùng timestamp/title | Thứ tự xác định theo ID |
| Đổi filter khi đang tải thêm | Generation cũ bị thay; UI không nối lẫn dataset |
| Sửa/xóa bảng đang được observe | LiveData/Flow/Paging cập nhật theo cơ chế tương ứng |
| Xoay máy/process recreation | Settings/DB giữ; search được restore theo saved state |
| View bị hủy rồi tạo lại | Không collector/callback giữ View cũ |
| Ghi/đọc gặp lỗi trong test | Không biến lỗi thành ghi thành công; UI có trạng thái phù hợp |
| Migration prefs/DB | Giá trị cũ giữ đúng; schema được validate |
| Transaction lỗi giữa chừng | Không có dữ liệu ghi một nửa |
| Font lớn/TalkBack | Control đọc được; list còn cuộn; action có ý nghĩa |

Nếu có project Android, các test có giá trị gồm DAO in-memory cho filter/sort/group, Flow emission sau insert/update/delete, migration validation và transaction rollback. Không dùng sleep cố định để “chờ Flow”; dùng coroutine testing/Flow testing phù hợp, assert emission hoặc điều kiện rõ ràng.

### 14.4 Rubric đánh giá intern

| Hạng mục | Điểm |
| --- | ---: |
| Chọn storage; đọc/ghi/xóa Preferences và migration | 20 |
| Entity/DAO/Database, CRUD và transaction | 20 |
| Query bind parameter, search, projection/query động | 15 |
| LiveData/Flow, MVVM và lifecycle | 15 |
| Pagination/Paging, load state và stable order | 15 |
| Sort/group đúng toàn dataset và group UI | 10 |
| Migration/test và giải thích bằng dữ liệu | 5 |
| **Tổng** | **100** |

Mức gợi ý đạt: từ 75 điểm; không còn query main-thread, sai identity/state, migration làm mất dữ liệu ngoài yêu cầu hoặc listener/collector giữ View cũ. Intern cần tự giải thích kết quả SQL thay vì chỉ chạy demo.

## 15. Lỗi thường gặp và câu hỏi ôn tập

| Hiện tượng | Nguyên nhân thường gặp | Hướng xử lý |
| --- | --- | --- |
| Database implementation không tồn tại | Chưa cấu hình Room compiler/KSP | Kiểm tra plugin, ksp dependency và lỗi generation |
| Đọc settings bị block UI | runBlocking hoặc commit trên main | Dùng Flow/suspend; chuyển ghi đồng bộ cũ khỏi main |
| DataStore báo nhiều instance | Tạo trùng store cùng file | Dùng một delegate/DI scope đúng |
| Settings mới và cũ khác nhau | Vẫn ghi SharedPreferences sau migration | Chọn một source of truth |
| UI không đổi sau insert | Chỉ query suspend một lần | Dùng observable query hoặc refresh có chủ đích |
| Flow thứ hai không được collect | Hai collect đặt tuần tự | Launch từng collector trong lifecycle block |
| Note state/click sai sau sort | Dùng position làm ID | Dùng primary key và DiffUtil |
| Phân trang trùng/bỏ sót | Order không ổn định hoặc dataset đổi khi offset tải | Tie-breaker; refresh/keyset/Paging theo yêu cầu |
| Sort chỉ đúng vài dòng đang thấy | Sort Kotlin sau khi lấy page | Sort tại query trước pagination |
| GROUP BY mất dòng detail | Aggregate trả nhóm, không phải list item | Tách query thống kê và query detail |
| Search % khớp mọi dòng | Chưa escape wildcard | Bind + escape + ESCAPE clause |
| Nâng app crash schema mismatch | Thiếu/sai migration/default/index | Validate schema và test upgrade thực tế |

1. **Preferences DataStore có thay Room không?** Không; storage cho settings nhỏ khác DB có quan hệ/query/cập nhật từng dòng.
2. **apply đã ghi disk xong khi hàm trả về chưa?** Không bảo đảm; phần ghi disk bất đồng bộ.
3. **suspend query có observable không?** Không; nó lấy một snapshot trong lần gọi đó.
4. **Flow query trả list rỗng có phải lỗi không?** Không; có thể không có dòng phù hợp.
5. **distinctUntilChanged ngăn query Room chạy lại không?** Không; nó lọc emission bằng nhau ở downstream.
6. **Vì sao ORDER BY cần id?** Để giải quyết trường hợp khóa sort chính trùng nhau và giữ thứ tự xác định.
7. **GROUP BY có giữ tất cả item không?** Không trong query aggregate mẫu; mỗi category trả một dòng thống kê.
8. **PagingConfig.pageSize có bằng mọi load size không?** Không; lần tải đầu và cơ chế tải có thể khác.
9. **cachedIn có lưu disk không?** Không; Room/DataStore mới giữ dữ liệu persistent trong bài.
10. **SQL migration chạy được đã đủ chưa?** Chưa; còn phải validate schema Room và giữ dữ liệu đúng yêu cầu.

## 16. Checklist hoàn thành và nguồn tài liệu

- [ ] Phân biệt SharedPreferences, Preferences DataStore và Room.
- [ ] Đọc/ghi/xóa settings; hiểu default, kiểu key, apply/commit và edit.
- [ ] Migrate prefs, dùng một instance store cho một file/process theo cấu hình.
- [ ] Thiết kế Entity/primary key/index/default và CRUD DAO.
- [ ] Viết WHERE/LIKE/IN/projection; bind giá trị, whitelist query structure.
- [ ] Phân biệt snapshot, LiveData, Flow và PagingSource.
- [ ] Collect theo View lifecycle; filter mới thay query cũ.
- [ ] Sort toàn dataset với tie-breaker; hiểu giới hạn NOCASE cho tiếng Việt.
- [ ] Dùng GROUP BY/HAVING để thống kê; group UI bằng detail/separator.
- [ ] Triển khai OFFSET/keyset/Paging và trạng thái load/retry/empty.
- [ ] Test migration/transaction/state sau recreation và lỗi đọc/ghi.

Các ví dụ, dữ liệu và bài tập được biên soạn cho intern. Nguồn dưới dùng để đối chiếu hành vi API/SQL; cần build và kiểm thử trong project trước khi dùng code làm sản phẩm.

| Nguồn chính thức | Nội dung đối chiếu |
| --- | --- |
| [SharedPreferences guide](https://developer.android.com/training/data-storage/shared-preferences) | Key–value, apply/commit và hướng chuyển sang DataStore |
| [DataStore guide](https://developer.android.com/topic/libraries/architecture/datastore) | Instance, Flow, edit và migration |
| [Room overview/setup](https://developer.android.com/training/data-storage/room) | Entity/DAO/Database và dependency |
| [Room DAO guide](https://developer.android.com/training/data-storage/room/accessing-data) | CRUD, Query, bind và projection |
| [Room async queries](https://developer.android.com/training/data-storage/room/async-queries) | Suspend, LiveData/Flow và invalidation |
| [Room relationships](https://developer.android.com/training/data-storage/room/relationships) | Quan hệ và đọc dữ liệu liên quan |
| [Upsert API](https://developer.android.com/reference/androidx/room/Upsert) | Insert/update theo primary key |
| [RawQuery API](https://developer.android.com/reference/androidx/room/RawQuery) | Query runtime và observedEntities |
| [Migration API](https://developer.android.com/reference/androidx/room/migration/Migration) | Migration giữa version DB |
| [Paging overview](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) | Pager, PagingSource và luồng dữ liệu |
| [PagingDataAdapter API](https://developer.android.com/reference/androidx/paging/PagingDataAdapter) | UI Views, submitData và load state |
| [Paging transformations — Views](https://developer.android.com/topic/libraries/architecture/views/paging/v3-transform-views) | map, separators và cachedIn |
| [SQLite SELECT](https://sqlite.org/lang_select.html) | ORDER BY, GROUP BY, HAVING và LIMIT/OFFSET |
| [SQLite expressions](https://www.sqlite.org/lang_expr.html) | LIKE, ESCAPE và collating behavior |
