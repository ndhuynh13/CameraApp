# Ứng Dụng Camera

Ứng dụng cá nhân: camera đa chức năng được phát triển bằng Kotlin và Jetpack Compose.

## Chức Năng Chính

### 1. Chụp Ảnh
- Chuyển đổi camera trước/sau
- Hẹn giờ chụp (3s, 5s, 10s)
- Bật/tắt đèn flash
- Tùy chỉnh tỷ lệ khung hình (4:3, 16:9, 1:1)
- Lưới hỗ trợ chụp ảnh (3x3)

### 2. Quay Video
- Quay video với chất lượng cao
- Hiển thị thời gian quay

### 3. Thư Viện Ảnh & Video
- Xem ảnh/video đã chụp
- Chế độ xem toàn màn hình
- Vuốt để chuyển ảnh/video
- Chọn nhiều ảnh/video để xóa
- Sắp xếp theo thời gian
- Zoom ảnh bằng vuốt và double tap vào màn hình

### 4. Chỉnh Sửa Ảnh
- Cắt và xoay ảnh
- Điều chỉnh màu sắc:
  - Độ sáng
  - Độ tương phản
  - Độ bão hòa
  - Độ sắc nét
  - Độ phơi sáng
  - Nhiệt độ màu
- Filter màu đa dạng
- Doodle: vẽ lên hình ảnh
## Chức Năng Phụ

### 1. Giao Diện
- Giao diện người dùng hiện đại với Jetpack Compose
- Hỗ trợ thao tác vuốt và cử chỉ
- Hiệu ứng chuyển đổi mượt với animation
- Chế độ xem toàn màn hình cho các image, video

### 2. Quản Lý Tập Tin
- Tự động lưu vào thư viện
- Định dạng tên file theo thời gian
- Hỗ trợ đa định dạng

### 3. Tối Ưu Hóa
- Xử lý ảnh không đồng bộ
- Tải ảnh hiệu quả với Coil
- Tương thích với Android 12 trở lên
- Xử lý quyền truy cập động
- Edit ảnh bằng GPUImage, Ucrop

## Yêu Cầu Hệ Thống
- Android SDK 31 trở lên
- Camera với tính năng tự động lấy nét
- Quyền truy cập: Camera, Bộ nhớ, Micro

## Thư Viện Sử Dụng
- CameraX
- Jetpack Compose
- Coil
- GPUImage
- uCrop
