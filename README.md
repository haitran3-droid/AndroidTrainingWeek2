# AndroidTrainingWeek2

Repository tổng hợp các project thực hành Android. **AquaDaily** là ứng dụng theo dõi lượng nước uống hằng ngày, được xây dựng bằng Kotlin, giao diện XML và kiến trúc MVVM.

## AquaDaily

AquaDaily giúp người dùng ghi lại từng lần uống nước, theo dõi tiến độ so với mục tiêu và xem lại lịch sử theo ngày. Dữ liệu được lưu trên thiết bị bằng Room; ứng dụng hoạt động ngoại tuyến và không cần đăng nhập.

### Tính năng chính

- **Home:** thêm lượng nước đã uống, chỉnh mục tiêu, xem tiến độ qua hiệu ứng sóng nước và theo dõi chuỗi ngày đạt mục tiêu.
- **Report:** chọn ngày xem lịch sử, kiểm tra lượng nước và thời gian của từng lần uống, xóa bản ghi nhập nhầm.
- **Infor:** đọc 9 bài viết thuộc các chuyên mục Cân bằng, Thói quen và Khỏe mạnh.
- Hỗ trợ giao diện sáng/tối và điều hướng bằng tab hoặc thao tác vuốt.

### Giao diện ứng dụng

Ảnh chụp thực tế trên điện thoại realme RMX3350 chạy Android 13. Dữ liệu 250 ml trong ảnh được tạo để minh họa.

<table>
  <tr>
    <th>Home — Tiến độ trong ngày</th>
    <th>Report — Lịch sử uống nước</th>
    <th>Infor — Kiến thức và thói quen</th>
  </tr>
  <tr>
    <td><img src="AquaDaily/docs/images/home.png" alt="Home: đã uống 250 ml, hoàn thành 13% mục tiêu 2.000 ml" width="240"></td>
    <td><img src="AquaDaily/docs/images/report.png" alt="Report: tổng lượng nước trong ngày và bản ghi uống 250 ml" width="240"></td>
    <td><img src="AquaDaily/docs/images/infor.png" alt="Infor: bài viết về kiến thức và thói quen uống nước" width="240"></td>
  </tr>
</table>

### Video demo

[Xem video demo AquaDaily](AquaDaily/docs/videos/aquadaily-demo.mp4)

Video dài khoảng **43 giây**, không có âm thanh, quay trực tiếp luồng **thêm 250 ml → xem tiến độ → xem lịch sử → mở bài viết**.

### Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ và giao diện | Kotlin, XML Layout, View Binding, Material Components |
| Kiến trúc và trạng thái | MVVM, ViewModel, SavedStateHandle, StateFlow |
| Điều hướng và danh sách | Fragment, ViewPager2, TabLayout, RecyclerView |
| Lưu trữ và bất đồng bộ | Room, KSP, Kotlin Coroutines, Flow |
| Giao diện tiến độ | Custom View: WaveView, GoalProgressView |

### Chạy project

1. Mở thư mục **AquaDaily** bằng Android Studio hỗ trợ Android Gradle Plugin **9.1.1**.
2. Chuẩn bị **JetBrains JDK 21**, Android SDK Platform **36.1** và đồng bộ Gradle.
3. Chọn thiết bị hoặc máy ảo chạy **Android 12 (API 31)** trở lên, rồi nhấn **Run**.

Để build APK bằng PowerShell từ thư mục gốc repository:

```powershell
cd AquaDaily
.\gradlew.bat :app:assembleDebug
```

Xem [README chi tiết của AquaDaily](AquaDaily/README.md) để biết cấu trúc mã nguồn, cách sử dụng, lệnh kiểm thử và cách xử lý lỗi JDK / `jlink`.

## Các project thực hành khác

- [DayOne](DayOne/)
- [DayTwo](DayTwo/)
- [DayFour](DayFour/)
- [DayFive](DayFive/)
- [DaySix](DaySix/)
