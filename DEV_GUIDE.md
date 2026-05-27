# TÀI LIỆU PHÁT TRIỂN - RESTAURANT RESERVATION SYSTEM
**Version:** 1.0 | **Ngày:** 27/05/2026 | **Ngôn ngữ:** Tiếng Việt

---

## MỤC LỤC

1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Kiến trúc & Quy tắc code](#2-kiến-trúc--quy-tắc-code)
3. [Database Schema](#3-database-schema)
4. [Danh sách API Endpoints](#4-danh-sách-api-endpoints)
5. [Tài liệu chi tiết Service Layer](#5-tài-liệu-chi-tiết-service-layer)
6. [Tài liệu chi tiết Controller Layer](#6-tài-liệu-chi-tiết-controller-layer)
7. [Tài liệu DataLoader](#7-tài-liệu-dataloader)
8. [Tài liệu Helper Classes](#8-tài-liệu-helper-classes)
9. [Luồng nghiệp vụ chính](#9-luồng-nghiệp-vụ-chính)
10. [Công nghệ sử dụng](#10-công-nghệ-sử-dụng)
11. [Hướng dẫn thực hành](#11-hướng-dẫn-thực-hành)
    - [11.1 Chuẩn bị môi trường](#111-chuẩn-bị-môi-trường)
    - [11.2 Tạo nhánh Git cho từng feature](#112-tạo-nhánh-git-cho-từng-feature)
    - [11.3 Chạy ứng dụng](#113-chạy-ứng-dụng)
    - [11.4 Kiểm thử API](#114-kiểm-thử-api)
    - [11.5 Lộ trình code theo thứ tự](#115-lộ-trình-code-theo-thứ-tự)

---

## 1. TỔNG QUAN HỆ THỐNG

### 1.1 Mô tả

Đây là hệ thống quản lý nhà hàng, hỗ trợ:
- **Quản lý người dùng** (admin, staff, client)
- **Quản lý bàn** (CRUD, soft delete)
- **Quản lý món ăn** (CRUD, phân loại theo bữa ăn)
- **Quản lý combo** (combo gồm nhiều món)
- **Đặt bàn** (khách hàng đặt bàn + chọn món/combo)
- **Đánh giá** (khách hàng để lại review)
- **JWT Authentication** (đăng nhập, đăng ký, xác thực email/OTP)
- **Real-time SSE** (thông báo cho admin khi có đặt bàn mới)

### 1.2 Vai trò người dùng

| Vai trò | Mô tả | Quyền |
|---------|--------|--------|
| `admin` | Quản trị viên | Full CRUD mọi thứ |
| `staff` | Nhân viên | CRUD bàn, món, combo, reservation |
| `client` | Khách hàng | Xem menu, đặt bàn, đánh giá |

### 1.3 Các bảng chính

```
users ─────┐
tables ────┼───── reservations ─────┬─── reservation_foods
foods ─────┼───── combos ────────┬──┼─── reservation_combos
combos ────┘    combo_foods ─────┘  └─── reservation_tables
reviews ─── users
```

---

## 2. KIẾN TRÚC & QUY TẮC CODE

### 2.1 Package Structure

```
com.example.qlnh/
├── models/
│   ├── entities/     - JPA Entity (ánh xạ bảng DB)
│   │   ├── BaseEntity.java       - Base id (Long id, auto-increment)
│   │   ├── AuditableEntity.java  - Base + created_at
│   │   ├── User.java
│   │   ├── Table.java
│   │   ├── Food.java
│   │   ├── Combo.java
│   │   ├── ComboFood.java        - Chi tiết combo (combo chứa nhiều món)
│   │   ├── Reservation.java
│   │   ├── ReservationFood.java   - Món ăn trong reservation
│   │   ├── ReservationCombo.java  - Combo trong reservation
│   │   ├── ReservationTable.java  - Bàn trong reservation
│   │   └── Review.java
│   └── enums/        - Enum constant
│       ├── UserRole.java
│       ├── FoodStatus.java
│       ├── MealType.java
│       ├── ReservationStatus.java
│       └── TableStatus.java
├── repositories/     - JPA Repository (truy vấn DB)
├── dto/
│   ├── request/     - Request body DTO
│   └── response/    - Response body DTO + ApiResponse<T>
├── services/
│   ├── interfaces/  - Contract cho service
│   │   ├── IUserService.java
│   │   ├── ITableService.java
│   │   ├── IFoodService.java
│   │   ├── IComboService.java
│   │   ├── IReservationService.java
│   │   ├── IReviewService.java
│   │   └── IEmailService.java
│   └── *.java       - Implementation (LOGIC Ở ĐÂY)
├── controllers/api/  - REST Controllers (API endpoints)
├── exception/       - Custom exception classes
├── config/           - Spring configuration
├── filter/           - JWT filter
├── helpers/          - Utility classes
└── aspect/           - AOP logging
```

### 2.2 Quy tắc annotation

```java
// Entity
@Entity                          // Đánh dấu là entity
@Table(name = "table_name")      // Ánh xạ tên bảng
@Id @GeneratedValue              // Primary key auto-increment
@MappedSuperclass                // Base class không phải entity

// Relationship
@ManyToOne(fetch = FetchType.LAZY)  // Quan hệ N-1, lazy load
@OneToMany                          // Quan hệ 1-N

// Service
@Service                          // Spring bean
@RequiredArgsConstructor          // Constructor injection (final fields)
@Transactional                   // Mở transaction
@Transactional(readOnly = true)   // Chỉ đọc, tối ưu performance
@Async                           // Chạy async (cho email)

// Controller
@RestController                  // REST API controller
@RequestMapping("/path")         // URL path
@GetMapping / @PostMapping / @PutMapping / @DeleteMapping
@RequiredArgsConstructor          // DI

// Validation
@NotBlank / @NotNull / @Email / @Min / @Max / @Size
@Valid                           // Kích hoạt validation ở request body
```

### 2.3 Quy tắc trả về API

```java
// Luôn dùng ApiResponse<T>
ApiResponse.success("Thông báo", data);     // 200 OK
ApiResponse.success("Thông báo");            // 200 OK (không có data)
ApiResponse.error("Lỗi");                   // 400/401/403/500

// Trong Controller
return ResponseEntity.ok(ApiResponse.success("OK", data));
return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi"));
return ResponseEntity.status(403).body(ApiResponse.error("Không có quyền"));
```

### 2.4 Quy tắc Exception

```java
// 404 - Không tìm thấy
throw new ResourceNotFoundException("User", "id", id);
// Kết quả: {"success":false,"message":"User not found with id: '123'"}

// 409 - Trùng lặp
throw new DuplicateResourceException("Email đã được sử dụng: " + email);

// 400 - Lỗi nghiệp vụ
throw new BusinessValidationException("Số người phải lớn hơn 0");

// GlobalExceptionHandler sẽ tự động bắt và trả về response phù hợp
```

---

## 3. DATABASE SCHEMA

### 3.1 Bảng `users`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| name | VARCHAR(255) | NO | Họ tên |
| email | VARCHAR(255) UNIQUE | NO | Email (duy nhất) |
| phone | VARCHAR(20) | YES | Số điện thoại |
| password | VARCHAR(255) | NO | BCrypt hash |
| role | VARCHAR(20) | NO | admin / staff / client |
| email_verified | BOOLEAN | NO | Đã xác thực email chưa |
| verification_token | VARCHAR(10) | YES | Mã OTP hoặc token xác thực |
| otp_expiry | TIMESTAMP | YES | Thời hạn OTP |
| created_at | TIMESTAMP | NO | Thời điểm tạo |

### 3.2 Bảng `tables`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| name | VARCHAR(100) | NO | Tên bàn (VD: "Bàn A1") |
| capacity | INT | NO | Số người tối đa |
| status | VARCHAR(20) | NO | available / occupied / reserved |
| location | VARCHAR(255) | YES | Vị trí (VD: "Tầng 1 - Cửa sổ") |
| deleted_at | TIMESTAMP | YES | Soft delete (NULL = đang hoạt động) |

> **Note:** Soft delete - xóa bàn = set `deleted_at = NOW()`, KHÔNG xóa vĩnh viễn.

### 3.3 Bảng `foods`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| name | VARCHAR(255) | NO | Tên món ăn |
| description | TEXT | YES | Mô tả |
| price | FLOAT | NO | Giá tiền (VND) |
| image_url | VARCHAR(500) | YES | URL ảnh |
| status | VARCHAR(20) | NO | available / unavailable |
| meal_type | VARCHAR(20) | YES | breakfast / lunch / dinner / dessert |
| created_by | BIGINT FK | NO | Người tạo (users.id) |
| created_at | TIMESTAMP | NO | Thời điểm tạo |

### 3.4 Bảng `combos`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| name | VARCHAR(255) | NO | Tên combo |
| price | FLOAT | NO | Giá combo |
| description | TEXT | YES | Mô tả |
| status | VARCHAR(20) | NO | available / unavailable |
| image_url | VARCHAR(500) | YES | URL ảnh |

### 3.5 Bảng `combo_foods`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| combo_id | BIGINT FK | NO | Combo cha |
| food_id | BIGINT FK | NO | Món ăn trong combo |
| quantity | INT | NO | Số lượng |

> **Note:** Combo có thể chứa cùng 1 món nhiều lần (quantity > 1).

### 3.6 Bảng `reservations`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| total_people | INT | NO | Số người |
| status | VARCHAR(20) | NO | pending / confirmed / cancelled / completed |
| reservation_at | TIMESTAMP | NO | Thời gian đặt bàn |
| note | TEXT | YES | Ghi chú (danh sách món đã đặt) |
| order_id | VARCHAR(36) UNIQUE | YES | UUID từ Redis (cho payment) |
| total_price | FLOAT | YES | Tổng tiền |
| customer_id | BIGINT FK | NO | Khách hàng (users.id) |
| table_id | BIGINT FK | YES | Bàn được gán |
| created_at | TIMESTAMP | NO | Thời điểm tạo |

### 3.7 Bảng `reservation_foods`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| reservation_id | BIGINT FK | NO | Reservation cha |
| food_id | BIGINT FK | NO | Món ăn |
| quantity | INT | NO | Số lượng |

### 3.8 Bảng `reservation_combos`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| reservation_id | BIGINT FK | NO | Reservation cha |
| combo_id | BIGINT FK | NO | Combo |
| quantity | INT | NO | Số lượng |

### 3.9 Bảng `reservation_tables`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| reservation_id | BIGINT FK | NO | Reservation cha |
| table_id | BIGINT FK | NO | Bàn |

### 3.10 Bảng `reviews`

| Cột | Kiểu | Nullable | Mô tả |
|-----|-------|----------|--------|
| id | BIGINT PK | NO | Auto-increment |
| customer_id | BIGINT FK | NO | Khách hàng (users.id) |
| rating | INT | NO | Điểm đánh giá (1-5) |
| content | TEXT | YES | Nội dung đánh giá |
| created_at | TIMESTAMP | NO | Thời điểm tạo |

---

## 4. DANH SÁCH API ENDPOINTS

### 4.1 Authentication - `/api/v1/auth`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| POST | `/login` | NO | Đăng nhập |
| POST | `/register` | NO | Đăng ký tài khoản mới |
| POST | `/verify-otp` | NO | Xác thực OTP |
| GET | `/verify-email` | NO | Xác thực email (qua link) |
| GET | `/me` | YES | Lấy thông tin user hiện tại |

### 4.2 Admin Users - `/api/v1/admin/users`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/` | ADMIN | Danh sách users (phân trang) |
| POST | `/` | ADMIN | Tạo user mới |
| PUT | `/{id}` | ADMIN | Cập nhật user |
| DELETE | `/{id}` | ADMIN | Xóa user |

### 4.3 Admin Tables - `/api/v1/admin/tables`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/all` | ADMIN | Tất cả bàn |
| GET | `/` | ADMIN | Danh sách bàn (phân trang) |
| POST | `/` | ADMIN | Tạo bàn mới |
| PUT | `/{id}` | ADMIN | Cập nhật bàn |
| DELETE | `/{id}` | ADMIN | Xóa bàn (soft delete) |

### 4.4 Admin Foods - `/api/v1/admin/foods`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/` | ADMIN/STAFF | Danh sách món (phân trang) |
| POST | `/` | ADMIN/STAFF | Tạo món mới |
| PUT | `/{id}` | ADMIN/STAFF | Cập nhật món |
| DELETE | `/{id}` | ADMIN/STAFF | Xóa món |

### 4.5 Admin Combos - `/api/v1/admin/combos`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/` | ADMIN/STAFF | Danh sách combo |
| GET | `/{id}` | ADMIN/STAFF | Chi tiết combo |
| POST | `/` | ADMIN/STAFF | Tạo combo |
| PUT | `/{id}` | ADMIN/STAFF | Cập nhật combo |
| DELETE | `/{id}` | ADMIN/STAFF | Xóa combo |

### 4.6 Admin Reservations - `/api/v1/admin/reservations`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/` | ADMIN/STAFF | Danh sách reservation |
| POST | `/` | ADMIN/STAFF | Tạo reservation thủ công |
| DELETE | `/{id}` | ADMIN/STAFF | Hủy reservation |
| PUT | `/{id}/assign-table` | ADMIN/STAFF | Gán bàn |

### 4.7 Client - `/api/v1/client/`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/foods` | NO | Danh sách món ăn |
| GET | `/combos` | NO | Danh sách combo |
| GET | `/tables/availability` | NO | Kiểm tra chỗ trống |
| POST | `/reservations` | NO | Đặt bàn |

### 4.8 SSE - `/api/v1/sse`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/admin` | ADMIN/STAFF | Subscribe SSE real-time |

### 4.9 Dashboard - `/api/v1/admin/stats`

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|--------|
| GET | `/revenue` | ADMIN | Thống kê doanh thu |

---

## 5. TÀI LIỆU CHI TIẾT SERVICE LAYER

---

### 5.1 UserService (`UserService.java`)

Package: `com.example.qlnh.services.UserService`
Interface: `IUserService`

#### 5.1.1 `registerClient(name, email, phone, password)`

**Mục đích:** Đăng ký tài khoản khách hàng mới

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| name | String | NOT NULL, NOT BLANK | Họ tên khách hàng |
| email | String | NOT NULL, NOT BLANK, UNIQUE | Email (duy nhất) |
| phone | String | NOT NULL, NOT BLANK | Số điện thoại |
| password | String | NOT NULL, NOT BLANK, >= 6 chars | Mật khẩu |

**ĐẦU RA:** `User` - Thông tin user đã tạo

**EXCEPTION:**
- `DuplicateResourceException` - Email đã tồn tại

**LOGIC CẦN THỰC HIỆN:**
```
1. Kiểm tra email đã tồn tại chưa (findByEmail)
   → Nếu tồn tại → ném DuplicateResourceException

2. Tạo mã OTP 6 số ngẫu nhiên
   → VD: "385921"

3. Tạo thời điểm hết hạn OTP = NOW + 15 phút

4. Tạo User entity:
   - name = tham số name
   - email = tham số email
   - phone = tham số phone
   - password = BCrypt encode tham số password
   - role = "client"
   - emailVerified = false
   - verificationToken = mã OTP
   - otpExpiry = thời điểm hết hạn
   - createdAt = NOW

5. Lưu user vào DB (save)

6. Gửi email OTP (sendOtpEmail)
   → Nên dùng @Async để không block request

7. Trả về User đã lưu
```

**VÍ DỤ:**
```java
@Override
@Transactional
public User registerClient(String name, String email, String phone, String password) {
    if (userRepository.findByEmail(email) != null) {
        throw new DuplicateResourceException("Email đã được sử dụng: " + email);
    }
    String otp = String.format("%06d", new Random().nextInt(999999));
    Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));

    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPhone(phone);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole("client");
    user.setEmailVerified(false);
    user.setVerificationToken(otp);
    user.setOtpExpiry(expiry);

    User saved = userRepository.save(user);
    emailService.sendOtpEmail(email, name, otp);
    return saved;
}
```

---

#### 5.1.2 `verifyOtp(email, otp)`

**Mục đích:** Xác thực mã OTP để kích hoạt tài khoản

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| email | String | NOT NULL | Email tài khoản |
| otp | String | NOT NULL, 6 chars | Mã OTP |

**ĐẦU RA:** `User` - User đã được xác thực

**EXCEPTION:**
- `ResourceNotFoundException` - Không tìm thấy tài khoản
- `BusinessValidationException` - OTP không đúng / đã hết hạn / đã xác thực rồi

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm user theo email (findByEmail)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Kiểm tra đã xác thực chưa (emailVerified)
   → Nếu true → ném BusinessValidationException("Tài khoản đã được xác nhận rồi")

3. Kiểm tra OTP có khớp không
   → Nếu verificationToken != otp → ném BusinessValidationException("Mã OTP không đúng")

4. Kiểm tra OTP còn hạn không
   → Nếu otpExpiry < NOW → ném BusinessValidationException("Mã OTP đã hết hạn")

5. Cập nhật user:
   - emailVerified = true
   - verificationToken = null
   - otpExpiry = null

6. Lưu user (save)
7. Trả về user
```

---

#### 5.1.3 `verifyEmail(token)`

**Mục đích:** Xác thực email qua link (token trong URL)

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| token | String | NOT NULL | Token từ email link |

**ĐẦU RA:** `User` - User đã được xác thực

**EXCEPTION:**
- `ResourceNotFoundException` - Token không hợp lệ / hết hạn

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm user theo verificationToken (findByVerificationToken)
   → Nếu null → ném ResourceNotFoundException("Verification token không hợp lệ")

2. Cập nhật:
   - emailVerified = true
   - verificationToken = null

3. Lưu user
4. Trả về user
```

---

#### 5.1.4 `deleteUser(id)`

**Mục đích:** Xóa user và tất cả dữ liệu liên quan

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| id | Long | NOT NULL | ID của user cần xóa |

**ĐẦU RA:** `void` (không trả về)

**EXCEPTION:**
- `ResourceNotFoundException` - User không tồn tại
- `BusinessValidationException` - Không được xóa admin

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm user theo id (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Kiểm tra role
   → Nếu role == "admin" → ném BusinessValidationException("Cannot delete user with ADMIN role")

3. Xóa cascade:
   a. Xóa reviews của user (reviewRepository.deleteByCustomerId(id))
   b. Tìm tất cả reservation IDs của user
      → Nếu có → xóa reservation (deleteAllByIdInBatch)
   c. Tìm tất cả food IDs do user tạo
      → Nếu có → xóa food (deleteAllByIdInBatch)
   d. Xóa user (userRepository.deleteById(id))

4. NOTE: Tất cả các bước trong CÙNG 1 transaction (@Transactional)
```

---

### 5.2 TableService (`TableService.java`)

Package: `com.example.qlnh.services.TableService`
Interface: `ITableService`

#### 5.2.1 `deleteTable(id)`

**Mục đích:** Xóa bàn (soft delete - KHÔNG xóa vĩnh viễn)

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| id | Long | NOT NULL | ID của bàn cần xóa |

**ĐẦU RA:** `void`

**EXCEPTION:**
- `ResourceNotFoundException` - Bàn không tồn tại

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm bàn theo id (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Soft delete:
   → set deletedAt = NOW (timestamp hiện tại)
   → Lưu bàn (save)

3. KHÔNG xóa các reservation liên quan (giữ lại lịch sử)
```

---

### 5.3 FoodService (`FoodService.java`)

Package: `com.example.qlnh.services.FoodService`
Interface: `IFoodService`

#### 5.3.1 `deleteFood(id)`

**Mục đích:** Xóa món ăn và dữ liệu liên quan

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| id | Long | NOT NULL | ID của món cần xóa |

**ĐẦU RA:** `void`

**EXCEPTION:**
- `ResourceNotFoundException` - Món không tồn tại

**LOGIC CẦN THỰC HIỆN:**
```
1. Kiểm tra món có tồn tại không (existsById)
   → Nếu không → ném ResourceNotFoundException

2. Xóa cascade:
   a. Xóa tất cả combo_foods liên quan đến food này
      → comboFoodRepository.deleteByFoodId(id)
   b. Xóa tất cả reservation_foods liên quan đến food này
      → reservationFoodRepository.deleteByFoodId(id)
   c. Xóa food
      → foodRepository.deleteById(id)
```

---

### 5.4 ComboService (`ComboService.java`)

Package: `com.example.qlnh.services.ComboService`
Interface: `IComboService`

#### 5.4.1 `createComboWithFoods(request)`

**Mục đích:** Tạo combo mới kèm danh sách món ăn trong combo

**ĐẦU VÀO:**
```java
ComboRequest {
    String name;           // Tên combo (VD: "Combo gia đình")
    Float price;          // Giá combo (VD: 450000)
    String description;     // Mô tả
    String status;         // available / unavailable
    String imageUrl;       // URL ảnh
    List<ComboFoodDto> foodItems;  // Danh sách món trong combo
        Long foodId;       // ID của món ăn
        Integer quantity;  // Số lượng
}
```

**ĐẦU RA:** `Combo` - Combo đã tạo kèm ID

**LOGIC CẦN THỰC HIỆN:**
```
1. Tạo Combo entity:
   - name = request.name
   - price = request.price
   - description = request.description
   - status = request.status (mặc định "available")
   - imageUrl = request.imageUrl

2. Lưu Combo vào DB → lấy saved Combo (có ID)

3. Nếu foodItems != null và không rỗng:
   a. Lấy danh sách foodId duy nhất (loại bỏ trùng lặp)
      → Dùng Set để deduplicate
   b. Tìm tất cả Food entity từ foodIds
      → foodRepository.findAllById(uniqueFoodIds)
      → Map<foodId, Food>
   c. Tạo danh sách ComboFood:
      → Với mỗi ComboFoodDto trong foodItems:
         - combo = savedCombo
         - food = foodMap.get(foodId)
         - quantity = dto.quantity (mặc định 1 nếu null)
   d. Lưu tất cả comboFoods
      → comboFoodRepository.saveAll(comboFoods)

4. Trả về savedCombo
```

---

#### 5.4.2 `updateComboWithFoods(id, request)`

**Mục đích:** Cập nhật combo và thay đổi danh sách món

**ĐẦU VÀO:**
| Tham số | Kiểu | Mô tả |
|---------|------|--------|
| id | Long | ID combo cần cập nhật |
| request | ComboRequest | Thông tin combo mới |

**ĐẦU RA:** `Combo` - Combo đã cập nhật

**EXCEPTION:**
- `ResourceNotFoundException` - Combo không tồn tại

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm combo hiện tại (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Cập nhật thông tin combo:
   - name, price, description, status, imageUrl = request.*

3. Lưu combo đã cập nhật

4. Xóa tất cả combo_foods cũ:
   → comboFoodRepository.deleteByComboId(savedCombo.id)

5. Tạo lại combo_foods mới (giống bước 3 ở createComboWithFoods)

6. Trả về savedCombo
```

---

#### 5.4.3 `deleteCombo(id)`

**Mục đích:** Xóa combo (chỉ khi chưa có reservation nào dùng combo này)

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| id | Long | NOT NULL | ID combo cần xóa |

**ĐẦU RA:** `boolean` - true nếu xóa thành công

**EXCEPTION:**
- `ResourceNotFoundException` - Combo không tồn tại
- `BusinessValidationException` - Combo đang được dùng trong reservation

**LOGIC CẦN THỰC HIỆN:**
```
1. Kiểm tra combo có tồn tại không
   → Nếu không → ném ResourceNotFoundException

2. Kiểm tra combo có đang được dùng trong reservation không
   → reservationComboRepository.existsByComboId(id)
   → Nếu true → ném BusinessValidationException("Cannot delete combo that is referenced in reservations")

3. Xóa combo (deleteById)

4. NOTE: combo_foods sẽ tự xóa do orphanRemoval=true hoặc cần xóa thủ công trước

5. Trả về true
```

---

### 5.5 ReservationService (`ReservationService.java`)

Package: `com.example.qlnh.services.ReservationService`
Interface: `IReservationService`

#### 5.5.1 `createReservation(name, email, phone, date, time, numberOfPeople, orderDetails, orderType, orderId)`

**Mục đích:** Tạo reservation mới - tìm bàn trống, tính tiền, lưu vào DB

Đây là method PHỨC TẠP NHẤT trong hệ thống.

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| name | String | NOT NULL, NOT BLANK | Tên khách |
| email | String | NOT NULL, NOT BLANK | Email khách |
| phone | String | NOT NULL, NOT BLANK | SĐT khách |
| date | String | Format: "YYYY-MM-DD" | Ngày đặt |
| time | String | Format: "HH:mm" | Giờ đặt |
| numberOfPeople | int | > 0 | Số người |
| orderDetails | String | NOT NULL | Chi tiết món đã đặt (JSON hoặc text) |
| orderType | String | "food" hoặc "combo" | Loại order |
| orderId | String | NULL (client) hoặc UUID (queue) | ID đơn hàng |

**ĐẦU RA:** `boolean` - true = thành công, false = thất bại

**EXCEPTION:**
- `BusinessValidationException` - Dữ liệu không hợp lệ
- `BusinessValidationException` - Ngoài giờ hoạt động (22:00-06:00)
- `BusinessValidationException` - Không còn bàn phù hợp

**LOGIC CẦN THỰC HIỆN:**
```
BƯỚC 1: VALIDATE ĐẦU VÀO
   - Kiểm tra tất cả trường NOT NULL
   - Nếu fail → ném BusinessValidationException

BƯỚC 2: TẠO HOẶC LẤY USER
   - Tìm user theo email hoặc phone (findByEmailOrPhone)
   - Nếu không tìm thấy → TẠO MỚI user:
       name = name param
       email = email param
       phone = phone param
       role = "customer"
       password = BCrypt(phone) [để khách đăng nhập sau bằng SĐT]
       createdAt = NOW
       → save(user)
   - Nếu tìm thấy nhưng name khác → CẬP NHẬT name

BƯỚC 3: PARSE ORDER DETAILS
   - orderDetails có thể là JSON: {"Cà phê sữa": 2, "Bánh flan": 1}
   - Hoặc text: "Cà phê sữa - 2 x\nBánh flan - 1 x"
   - Parse thành List<OrderItem> {foodName, quantity}

BƯỚC 4: TẠO TIMESTAMP ĐẶT BÀN
   - date + time → Timestamp
   - Format: "2026-05-27" + "19:00" → "2026-05-27 19:00:00"

BƯỚC 5: KIỂM TRA GIỜ HOẠT ĐỘNG
   - Giờ đặt phải trong khoảng 06:00 - 22:00
   - Nếu ngoài → ném BusinessValidationException("Nhà hàng không phục vụ trong khung giờ 22:00 - 06:00")

BƯỚC 6: TÍNH THỜI GIAN KHUNG GIỜ
   - diningEnd = reservationAt + 2 tiếng
   - overlapStart = reservationAt - 2 tiếng
   (Tức: nếu đặt 19h, kiểm tra bàn trống trong khoảng 17h-21h)

BƯỚC 7: TÌM BÀN TRỐNG
   - Gọi tableRepository.findAvailableTableIds(
       numberOfPeople,    // số người
       diningEnd,         // giới hạn kết thúc
       overlapStart       // giới hạn bắt đầu
     )
   - Trả về danh sách ID bàn có thể đặt, ưu tiên bàn nhỏ nhất
   - Nếu danh sách rỗng → ném BusinessValidationException("Không còn bàn phù hợp")

BƯỚC 8: TÍNH TỔNG TIỀN
   - Nếu orderType == "combo":
       → Tìm tất cả combo theo name trong orderDetails
       → Tính tổng: combo.price * quantity
   - Nếu orderType == "food" (mặc định):
       → Tìm tất cả food theo name trong orderDetails
       → Tính tổng: food.price * quantity

BƯỚC 9: TẠO RESERVATION ENTITY
   - customer = user bước 2
   - totalPeople = numberOfPeople
   - reservationAt = timestamp bước 4
   - note = orderDetails (lưu để biết khách đặt gì)
   - totalPrice = tổng tiền bước 8
   - orderId = orderId (UUID từ Redis queue)
   - status = "confirmed" (hoặc "pending" tùy business)
   → Lưu reservation → lấy reservation có ID

BƯỚC 10: LƯU CHI TIẾT ORDER
   - Nếu orderType == "combo":
       → Với mỗi combo trong order: tạo ReservationCombo
       → reservationComboRepository.createReservationCombo(resId, comboId, quantity)
       [Hoặc save vào entity rồi repository.saveAll()]
   - Nếu orderType == "food":
       → Với mỗi food trong order: tạo ReservationFood
       → reservationFoodRepository.createReservationFood(resId, foodId, quantity)

BƯỚC 11: TRẢ VỀ true
```

**VÍ DỤ INPUT:**
```java
createReservation(
    "Nguyen Van A",                    // name
    "nguyenvana@email.com",            // email
    "0901234567",                       // phone
    "2026-05-30",                     // date
    "19:00",                           // time
    4,                                  // numberOfPeople
    "{\"Mì Ý sốt bò bằm\": 2, \"Salad trộn\": 2}",  // orderDetails (JSON)
    "food",                            // orderType
    null                               // orderId (không dùng Redis)
);
```

**VÍ DỤ ORDER DETAILS JSON:**
```json
{
  "Mì Ý sốt bò bằm": 2,
  "Salad trộn": 2,
  "Nước cam tươi": 2
}
```

---

#### 5.5.2 `assignTable(reservationId, tableId)`

**Mục đích:** Admin gán bàn cho reservation

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| reservationId | Long | NOT NULL | ID reservation |
| tableId | Long | NOT NULL | ID bàn muốn gán |

**ĐẦU RA:** `boolean` - true = thành công

**EXCEPTION:**
- `ResourceNotFoundException` - Reservation/Bàn không tồn tại
- `BusinessValidationException` - Bàn không trống

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm reservation (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Tìm bàn (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

3. Kiểm tra bàn có trống không
   → Nếu status != "available" → ném BusinessValidationException("Table is not available")

4. Cập nhật bàn:
   → table.status = "reserved"
   → tableRepository.save(table)

5. Cập nhật reservation:
   → reservation.table = table
   → reservation.status = "confirmed"
   → reservationRepository.save(reservation)

6. Trả về true
```

---

#### 5.5.3 `deleteReservationById(reservationId)`

**Mục đích:** Hủy reservation (không xóa vĩnh viễn, chỉ đổi status)

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| reservationId | Long | NOT NULL | ID reservation cần hủy |

**ĐẦU RA:** `void`

**EXCEPTION:**
- `ResourceNotFoundException` - Reservation không tồn tại

**LOGIC CẦN THỰC HIỆN:**
```
1. Tìm reservation (findById)
   → Nếu không tìm thấy → ném ResourceNotFoundException

2. Cập nhật:
   → reservation.status = "cancelled"

3. Lưu reservation (save)

4. NOTE: Không xóa reservation_foods, reservation_combos vì cần giữ lịch sử
```

---

### 5.6 ReviewService (`ReviewService.java`)

Package: `com.example.qlnh.services.ReviewService`
Interface: `IReviewService`

#### 5.6.1 `createReview(name, email, phone, content)`

**Mục đích:** Tạo đánh giá từ khách hàng

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| name | String | NOT NULL, NOT BLANK | Tên khách |
| email | String | NOT NULL, NOT BLANK | Email khách |
| phone | String | NOT NULL, NOT BLANK | SĐT khách |
| content | String | NOT NULL, NOT BLANK | Nội dung đánh giá |

**ĐẦU RA:** `boolean` - true = thành công

**LOGIC CẦN THỰC HIỆN:**
```
1. Validate: tất cả trường phải có giá trị
   → Nếu fail → log warn + return false

2. Tìm user theo email/phone (findByEmailOrPhone)
   → Nếu không tìm thấy → TẠO MỚI user (giống bước 2 trong createReservation)
       role = "customer"
       password = BCrypt(phone)
   → Nếu tìm thấy nhưng name khác → CẬP NHẬT name

3. Tạo Review entity:
   - customer = user bước 2
   - rating = 5 (mặc định, có thể mở rộng cho phép khách chọn)
   - content = content param
   - createdAt = NOW

4. Lưu review (save)
5. Trả về true

NOTE: Rating mặc định = 5. Có thể mở rộng sau bằng cách thêm rating vào request.
```

---

### 5.7 EmailService (`EmailService.java`)

Package: `com.example.qlnh.services.EmailService`
Interface: `IEmailService`

#### 5.7.1 `sendVerificationEmail(toEmail, name, token)`

**Mục đích:** Gửi email xác thực tài khoản qua link

**ĐẦU VÀO:**
| Tham số | Kiểu | Mô tả |
|---------|------|--------|
| toEmail | String | Email người nhận |
| name | String | Tên người nhận |
| token | String | Token xác thực (sẽ đính trong link) |

**ĐẦU RA:** `void` (gửi email, không trả về)

**LOGIC CẦN THỰC HIỆN:**
```
1. Tạo MimeMessage từ mailSender
2. Tạo MimeMessageHelper (set HTML = true)
3. Set thông tin:
   - from = spring.mail.username
   - to = toEmail
   - subject = "Xác nhận tài khoản - Nhà Hàng"
   - verifyUrl = app.frontend.url + "/verify-email?token=" + token
     VD: "http://localhost:5173/verify-email?token=ABC123"

4. Tạo HTML body (template email):
   - Chào mừng name
   - Nút/link "Xác nhận tài khoản" → verifyUrl
   - Thiết kế HTML đẹp (inline CSS)

5. Set text với HTML: helper.setText(html, true)

6. Gửi: mailSender.send(message)

7. NOTE: Nên dùng @Async để không block request:
   @Async
   @Override
   public void sendVerificationEmail(...) { ... }
```

**VÍ DỤ HTML EMAIL:**
```html
<div style="font-family: 'Segoe UI', sans-serif; max-width: 500px; margin: 0 auto; padding: 2rem; background: #f8f9fc; border-radius: 12px;">
    <h2 style="color: #1a1a2e; text-align: center;">Chào mừng đến Nhà Hàng!</h2>
    <p style="color: #5a5c69;">Xin chào <strong>Nguyen Van A</strong>,</p>
    <p style="color: #5a5c69;">Cảm ơn bạn đã đăng ký. Vui lòng nhấn nút bên dưới để xác nhận.</p>
    <div style="text-align: center; margin: 2rem 0;">
        <a href="http://localhost:5173/verify-email?token=ABC123"
           style="background: linear-gradient(135deg,#e74a3b,#f6c23e); color: #fff; padding: 0.85rem 2rem; border-radius: 50px; text-decoration: none; font-weight: 700;">
            Xác nhận tài khoản
        </a>
    </div>
</div>
```

---

#### 5.7.2 `sendOtpEmail(toEmail, name, otp)`

**Mục đích:** Gửi email chứa mã OTP 6 số

**ĐẦU VÀO:**
| Tham số | Kiểu | Mô tả |
|---------|------|--------|
| toEmail | String | Email người nhận |
| name | String | Tên người nhận |
| otp | String | Mã OTP 6 số |

**ĐẦU RA:** `void`

**LOGIC CẦN THỰC HIỆN:**
```
1-5. Giống sendVerificationEmail

6. Tạo HTML body với mã OTP nổi bật:
   - Hiển thị OTP với font lớn, đậm
   - VD: OTP = "385921" → hiển thị "3 8 5 9 2 1"
   - Thông báo: "Mã có hiệu lực trong 15 phút"

7. Gửi email

8. NOTE: Dùng @Async
```

---

## 6. TÀI LIỆU CHI TIẾT CONTROLLER LAYER

---

### 6.1 AuthApiController (`AuthApiController.java`)

Base path: `/api/v1/auth`

#### 6.1.1 `POST /login`

**Mục đích:** Đăng nhập, trả về JWT token

**Request Body:**
```json
{
  "email": "admin@gmail.com",
  "password": "12345678"
}
```

**ĐẦU VÀO:** `Map<String, String>` với email, password

**ĐẦU RA:** `ApiResponse<Map<String, Object>>`
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "admin@gmail.com",
    "role": "admin",
    "name": "Admin User"
  }
}
```

**LOGIC CẦN THỰC HIỆN:**
```
1. Validate: email và password không rỗng
   → Nếu rỗng → badRequest

2. Xác thực credentials:
   → authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, password)
     )
   → Nếu fail (AuthenticationException) → trả 401

3. Tìm user theo email (userService.getUserByEmail)
   → Kiểm tra emailVerified == true
   → Nếu chưa xác thực → trả 403 "Email chưa được xác nhận"

4. Tạo JWT token:
   → jwtTokenProvider.generateToken(authentication)

5. Trả về ApiResponse với token + user info
```

---

#### 6.1.2 `POST /register`

**Mục đích:** Đăng ký tài khoản khách hàng mới

**Request Body:**
```json
{
  "name": "Nguyen Van B",
  "email": "nguyenvanb@email.com",
  "phone": "0912345678",
  "password": "12345678",
  "confirmPassword": "12345678"
}
```

**ĐẦU RA:** `ApiResponse`
```json
{
  "success": true,
  "message": "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản.",
  "data": {
    "email": "nguyenvanb@email.com"
  }
}
```

**LOGIC CẦN THỰC HIỆN:**
```
1. Validate input:
   - Tất cả trường NOT NULL
   - password == confirmPassword
   - password.length >= 6

2. Gọi userService.registerClient(name, email, phone, password)
   → Nếu ném DuplicateResourceException → badRequest

3. Trả về success response
```

---

#### 6.1.3 `POST /verify-otp`

**Mục đích:** Xác thực mã OTP

**Request Body:**
```json
{
  "email": "nguyenvanb@email.com",
  "otp": "385921"
}
```

**ĐẦU RA:** `ApiResponse`
```json
{
  "success": true,
  "message": "Xác nhận email thành công!",
  "data": {
    "email": "nguyenvanb@email.com",
    "name": "Nguyen Van B"
  }
}
```

**LOGIC CẦN THỰC HIỆN:**
```
1. Validate: email và otp không rỗng

2. Gọi userService.verifyOtp(email, otp)
   → Bắt exception → badRequest với message

3. Trả về success
```

---

#### 6.1.4 `GET /verify-email?token=ABC123`

**Mục đích:** Xác thực email qua link

**Query Param:** `token` - Token từ email

**ĐẦU RA:** `ApiResponse`

**LOGIC CẦN THỰC HIỆN:**
```
1. Gọi userService.verifyEmail(token)
   → Bắt exception → badRequest

2. Trả về success
```

---

#### 6.1.5 `GET /me`

**Mục đích:** Lấy thông tin user hiện tại (từ JWT)

**ĐẦU RA:** `ApiResponse`
```json
{
  "success": true,
  "data": {
    "email": "admin@gmail.com",
    "name": "Admin User",
    "role": "admin"
  }
}
```

**LOGIC CẦN THỰC HIỆN:**
```
1. Lấy Authentication từ SecurityContext
   → Nếu null hoặc not authenticated → 401

2. Lấy email từ auth.getName()
   → Tìm user (userService.getUserByEmail)

3. Trả về user info
```

---

### 6.2 UserApiController (`UserApiController.java`)

Base path: `/api/v1/admin/users`
Auth: ADMIN only

#### 6.2.1 `GET /` - Danh sách users

**Query Params:**
| Param | Kiểu | Default | Mô tả |
|-------|------|---------|--------|
| page | int | 1 | Trang (1-based) |
| itemsPerPage | int | 10 | Số item/trang |
| role | String | null | Lọc theo role |
| keyword | String | null | Tìm kiếm (name/email/phone) |

**ĐẦU RA:** `ApiResponse<Page<UserResponse>>`

**LOGIC CẦN THỰC HIỆN:**
```
1. Xác định query:
   - Có keyword + có role → findByKeywordWithRole
   - Có keyword → findByKeyword
   - Có role → getUsersByRole
   - Không có gì → getUsers

2. Gọi service tương ứng

3. Map sang UserResponse.fromEntity() cho mỗi user

4. Trả về Page<UserResponse>
```

---

#### 6.2.2 `POST /` - Tạo user

**Request Body:** `CreateUserRequest`
```json
{
  "name": "Staff User",
  "email": "staff@qlnh.com",
  "phone": "0909876543",
  "role": "staff",
  "password": "12345678",
  "confirmPassword": "12345678"
}
```

**ĐẦU RA:** `ApiResponse<Void>`

**LOGIC CẦN THỰC HIỆN:**
```
1. Validate: @Valid annotation (trong DTO đã khai báo @NotBlank, @Email)

2. Tạo User entity từ request

3. Validate nghiệp vụ:
   - checkEmailExist(user) → true → badRequest("Email already exists")
   - checkPhoneExist(user) → true → badRequest("Phone number already exists")
   - comparePassword(password, confirmPassword) → false → badRequest("Passwords do not match")

4. Mã hóa password:
   → user.setPassword(passwordEncoder.encode(password))

5. Set emailVerified = true (admin tạo → không cần xác thực)

6. Lưu: userService.createUser(user)

7. Trả về success
```

---

#### 6.2.3 `PUT /{id}` - Cập nhật user

**Request Body:** `UpdateUserRequest`
```json
{
  "name": "Staff User Updated",
  "email": "staff@qlnh.com",
  "phone": "0909876543",
  "role": "staff",
  "password": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

**ĐẦU RA:** `ApiResponse<Void>`

**LOGIC CẦN THỰC HIỆN:**
```
1. Lấy user hiện tại: userService.getUserById(id)

2. Áp dụng update: set name, email, phone, role

3. Validate:
   - checkEmailExist(updated) → true → badRequest
   - checkPhoneExist(updated) → true → badRequest

4. Nếu password có giá trị:
   - comparePassword → false → badRequest
   - Encode password
   - Gán vào updated
5. Nếu password trống → GIỮ NGUYÊN password cũ

6. Lưu: userService.updateUser(updated)

7. Nếu là user đang login và email thay đổi:
   → Trả message "User updated. Please login again."
```

---

#### 6.2.4 `DELETE /{id}` - Xóa user

**Path:** `DELETE /api/v1/admin/users/{id}`
**Auth:** ADMIN only

**ĐẦU RA:** `ApiResponse<Void>`

**LOGIC CẦN THỰC HIỆN:**
```
1. Lấy user hiện tại đang login (currentUserResolver.resolve())

2. Kiểm tra:
   → Nếu xóa chính mình (currentUser.id == id && role == admin)
     → badRequest("Cannot delete currently logged-in admin account")

3. Gọi userService.deleteUser(id)

4. Trả về success
```

---

### 6.3 TableApiController (`TableApiController.java`)

Base path: `/api/v1/admin/tables`
Auth: ADMIN/STAFF

#### 6.3.1 `GET /all` - Tất cả bàn

**ĐẦU RA:** `ApiResponse<List<TableResponse>>`
```json
{
  "success": true,
  "data": [
    {"id": 1, "name": "Bàn A1", "capacity": 2, "status": "available", "location": "Tầng 1"},
    {"id": 2, "name": "Bàn A2", "capacity": 4, "status": "occupied", "location": "Tầng 1"}
  ]
}
```

**LOGIC:**
```
1. Gọi tableService.getAllTables()
2. Map sang TableResponse.fromEntity()
3. Trả về List
```

---

#### 6.3.2 `GET /` - Danh sách bàn (phân trang)

**Query Params:** page, itemsPerPage, status, keyword

**LOGIC:**
```
1. Xác định query (4 trường hợp):
   - keyword + status → findByKeywordAndStatus
   - keyword → findByKeyword
   - status → getTablesByPageAndStatus
   - không có → getTablesByPage

2. Map sang TableResponse

3. Trả về Page
```

---

#### 6.3.3 `POST /` - Tạo bàn

**Request Body:** `TableRequest`
```json
{
  "name": "Bàn D1",
  "capacity": 6,
  "status": "available",
  "location": "Tầng 2 - Cửa sổ"
}
```

**LOGIC:**
```
1. Validate: @Valid

2. Tạo Table entity từ request

3. Lưu: tableService.createTable(table)

4. Trả về TableResponse
```

---

#### 6.3.4 `PUT /{id}` - Cập nhật bàn

**Request Body:** `TableRequest`

**LOGIC:**
```
1. Lấy bàn hiện tại: tableService.getTableById(id)
2. Áp dụng update: name, capacity, status, location
3. Lưu: tableService.updateTable(table)
4. Trả về TableResponse
```

---

#### 6.3.5 `DELETE /{id}` - Xóa bàn (soft delete)

**LOGIC:**
```
1. Gọi tableService.deleteTable(id)
2. Trả về success
```

---

### 6.4 FoodApiController (`FoodApiController.java`)

Base path: `/api/v1/admin/foods`
Auth: ADMIN/STAFF

#### 6.4.1 `GET /` - Danh sách món

**Query Params:** page, itemsPerPage, status, mealType, keyword

**ĐẦU RA:** `ApiResponse<Page<FoodResponse>>`

**LOGIC:**
```
1. Xác định query (nhiều trường hợp):
   - keyword + status + mealType → findByKeywordAndStatusAndMealType
   - keyword + status → findByKeywordAndStatus
   - keyword + mealType → findByKeywordAndMealType
   - status + mealType → getFoodsByStatusAndMealType
   - keyword → findByKeyword
   - status → getFoodsByStatus
   - mealType → getFoodsByMealType
   - không có → getFoods

2. Lấy user hiện tại: currentUserResolver.resolve()
   → Dùng để set createdBy khi tạo món

3. Map sang FoodResponse (FoodResponse.fromEntity)
   → Lưu ý: createdByName = food.createdBy.name

4. Trả về Page<FoodResponse>
```

---

#### 6.4.2 `POST /` - Tạo món mới

**Request Body:** `FoodRequest`
```json
{
  "name": "Cà phê đen",
  "description": "Cà phê đen nguyên chất",
  "price": 29000,
  "imageUrl": "https://example.com/coffee.jpg",
  "status": "available",
  "mealType": "breakfast"
}
```

**LOGIC:**
```
1. Validate: @Valid

2. Lấy current user: currentUserResolver.resolve()
   → Nếu null → badRequest

3. Tạo Food entity:
   - name, description, price, imageUrl, status, mealType = request.*
   - createdBy = currentUser

4. Lưu: foodService.createFood(food)

5. Trả về FoodResponse
```

---

#### 6.4.3 `PUT /{id}` - Cập nhật món

**Request Body:** `FoodRequest`

**LOGIC:**
```
1. Lấy food hiện tại: foodService.getFoodById(id)
2. Áp dụng update: name, description, price, imageUrl, status, mealType
   → KHÔNG thay đổi createdBy
3. Lưu: foodService.updateFood(existing)
4. Trả về FoodResponse
```

---

#### 6.4.4 `DELETE /{id}` - Xóa món

**LOGIC:**
```
1. Gọi foodService.deleteFood(id)
2. Trả về success
```

---

### 6.5 ComboApiController (`ComboApiController.java`)

Base path: `/api/v1/admin/combos`
Auth: ADMIN/STAFF

#### 6.5.1 `GET /` - Danh sách combo

**Query Params:** page, itemsPerPage, status, keyword

**LOGIC:** Giống pattern các controller khác (4 trường hợp filter)

---

#### 6.5.2 `GET /{id}` - Chi tiết combo

**ĐẦU RA:** `ApiResponse<ComboResponse>`
```json
{
  "data": {
    "id": 1,
    "name": "Combo cặp đôi",
    "price": 250000,
    "description": "Combo dành cho 2 người",
    "status": "available",
    "imageUrl": "https://...",
    "foodItems": [
      {"foodId": 6, "quantity": 2},
      {"foodId": 4, "quantity": 2}
    ]
  }
}
```

**LOGIC:**
```
1. Lấy combo: comboService.getComboById(id)
2. Lấy combo_foods: comboService.getComboFoods(id)
3. Map: ComboResponse.fromEntity(combo, comboFoods)
4. Trả về
```

---

#### 6.5.3 `POST /` - Tạo combo

**Request Body:** `ComboRequest`
```json
{
  "name": "Combo tiệc nhỏ",
  "price": 850000,
  "description": "Combo dành cho 8 người",
  "status": "available",
  "imageUrl": "https://...",
  "foodItems": [
    {"foodId": 7, "quantity": 2},
    {"foodId": 8, "quantity": 3},
    {"foodId": 9, "quantity": 3}
  ]
}
```

**LOGIC:**
```
1. Gọi comboService.createComboWithFoods(request)
2. Lấy comboFoods: comboService.getComboFoods(saved.id)
3. Trả về ComboResponse.fromEntity(saved, foods)
```

---

#### 6.5.4 `PUT /{id}` - Cập nhật combo

**LOGIC:**
```
1. Gọi comboService.updateComboWithFoods(id, request)
2. Lấy comboFoods mới
3. Trả về ComboResponse
```

---

#### 6.5.5 `DELETE /{id}` - Xóa combo

**LOGIC:**
```
1. Gọi comboService.deleteCombo(id)
2. NOTE: deleteCombo ném BusinessValidationException nếu combo đang dùng trong reservation
   → Bắt exception → trả badRequest
3. Trả về success
```

---

### 6.6 ReservationApiController (`ReservationApiController.java`)

Base path: `/api/v1/admin/reservations`
Auth: ADMIN/STAFF

#### 6.6.1 `GET /` - Danh sách reservation

**Query Params:** page, itemsPerPage, status, keyword, date

**ĐẦU RA:** `ApiResponse<Page<ReservationResponse>>`

**LOGIC:**
```
1. Gọi reservationService.searchReservations(keyword, status, date, page, itemsPerPage)
   → Lưu ý: Cần xử lý date string → Timestamp

2. Map sang ReservationResponse.fromEntity()

3. Trả về Page
```

---

#### 6.6.2 `POST /` - Tạo reservation thủ công

**Request Body:** `CreateReservationRequest`

**LOGIC:**
```
1. Validate: @Valid

2. Gọi reservationService.createReservation(
     req.name, req.email, req.phone, req.date, req.time,
     req.numberOfPeople, req.orderDetails, req.orderType, null
   )
   → Nếu ném exception → badRequest với message

3. Trả về success
```

---

#### 6.6.3 `DELETE /{id}` - Hủy reservation

**LOGIC:**
```
1. Gọi reservationService.deleteReservationById(id)
2. Trả về success
```

---

#### 6.6.4 `PUT /{id}/assign-table` - Gán bàn

**Request Body:**
```json
{
  "tableId": 3
}
```

**LOGIC:**
```
1. Lấy tableId từ body
   → Nếu null → badRequest("tableId is required")

2. Gọi reservationService.assignTable(id, tableId)
   → Bắt exception → badRequest

3. Trả về success
```

---

### 6.7 Client APIs

#### 6.7.1 `GET /api/v1/client/foods` - Danh sách món (public)

**Query Params:** page, itemsPerPage, mealType

**ĐẦU RA:** `ApiResponse<List<FoodResponse>>`

**LOGIC:**
```
1. Chỉ hiển thị món có status = "available"

2. Phân trang:
   - Có mealType → getFoodsByStatusAndMealType(page, itemsPerPage, "available", mealType)
   - Không có → getFoodsByStatus(page, itemsPerPage, "available")

3. Map và trả về List<FoodResponse>
```

---

#### 6.7.2 `GET /api/v1/client/combos` - Danh sách combo (public)

**Query Params:** page, itemsPerPage

**LOGIC:**
```
1. Chỉ hiển thị combo có status = "active" (hoặc "available" tùy convention)

2. Gọi: comboService.getCombosByPageAndStatus(page, itemsPerPage, "active")

3. Map mỗi combo sang ComboResponse (cần lấy thêm combo_foods)

4. Trả về Page<ComboResponse>
```

---

#### 6.7.3 `GET /api/v1/client/tables/availability?date=2026-05-30&time=19:00`

**Mục đích:** Kiểm tra số chỗ trống cho 1 khung giờ

**ĐẦU VÀO:**
| Param | Kiểu | Ràng buộc | Mô tả |
|-------|------|-----------|--------|
| date | String | YYYY-MM-DD | Ngày |
| time | String | HH:mm | Giờ |

**ĐẦU RA:** `ApiResponse<Map<String, Object>>`
```json
{
  "success": true,
  "data": {
    "date": "2026-05-30",
    "time": "19:00",
    "availableSeats": 24,
    "queriedAt": "2026-05-27T23:45:00"
  }
}
```

**LOGIC:**
```
1. Validate: date và time không rỗng

2. Gọi availabilityService.getAvailableSeats(date, time)
   → Trả về tổng số chỗ trống (capacity của các bàn trống)

3. Trả về Map với date, time, availableSeats
```

---

#### 6.7.4 `POST /api/v1/client/reservations` - Đặt bàn (public)

**Request Body:** `CreateReservationRequest`

**ĐẦU RA:**
- 202 Accepted: Đang xử lý
- 400: Validation failed
- 503: Hệ thống bận

**LOGIC CẦN THỰC HIỆN (PHỨC TẠP):**
```
1. Gọi payloadResolver.resolveUserInfo(request, auth)
   → Nếu user đã login (JWT hợp lệ) → tự điền name, email, phone

2. Gọi payloadResolver.resolveDatetime(request)
   → Nếu có reservationAt nhưng không có date/time → tách ra

3. Gọi payloadResolver.resolveOrderDetails(request)
   → Nếu orderDetails trống → dùng note
   → Nếu cả hai trống → gán "Không có món đặt trước"

4. Gọi validator.validate(request)
   → Bắt validation error → trả 400

5. [TÙY CHỌN: Kiểm tra & deduct inventory từ Redis]
   → Nếu dùng Redis: redisService.deductInventory(date, time, people)
   → Invalidate cache: availabilityService.invalidateCache(date, time)

6. Gọi reservationService.createReservation(...)
   → Nếu ném exception → trả 400 với message

7. Trả về 202 Accepted với orderId
   → orderId = UUID.randomUUID().toString()
```

---

### 6.8 SseController (`SseController.java`)

#### 6.8.1 `GET /api/v1/sse/admin` - Subscribe SSE

**Mục đích:** Admin subscribe để nhận thông báo real-time

**ĐẦU VÀO:**
| Param | Kiểu | Default | Mô tả |
|-------|------|---------|--------|
| clientId | String | "anonymous" | ID client để track |

**ĐẦU RA:** `SseEmitter` (Server-Sent Events stream)

**LOGIC:**
```
1. Gọi sseEmitterService.register(clientId)
   → Tạo SseEmitter với timeout 5 phút
   → Đăng ký callback: onCompletion, onTimeout, onError
   → Gửi event "connected" với clientId
   → Thêm vào ConcurrentHashMap

2. Trả về emitter

3. Frontend sẽ listen các events:
   - "new_reservation" - Có đặt bàn mới
   - "payment_confirmed" - Thanh toán xác nhận
   - "reservation_cancelled" - Đặt bàn bị hủy
   - "seat_count_update" - Số chỗ thay đổi
   - "heartbeat" - Ping định kỳ (25s)
```

---

### 6.9 DashboardApiController (`DashboardApiController.java`)

#### 6.9.1 `GET /api/v1/admin/stats/revenue` - Thống kê doanh thu

**ĐẦU RA:** `ApiResponse<Map<String, Object>>`
```json
{
  "success": true,
  "data": {
    "totalRevenue": 15000000.0,
    "monthlyCancelledRevenue": {
      "2026-03": 500000.0,
      "2026-04": 1200000.0
    }
  }
}
```

**LOGIC:**
```
1. Gọi reservationService.getTotalRevenue()
   → SUM(totalPrice) WHERE status = 'cancelled'

2. [TÙY CHỌN] Lấy monthlyCancelledRevenue
   → GROUP BY MONTH/YEAR WHERE status = 'cancelled'

3. Trả về Map
```

---

## 7. TÀI LIỆU DATALOADER

### 7.1 Mục đích

Chạy **một lần duy nhất** khi database trống (`userRepository.count() == 0`). Tạo dữ liệu mẫu ban đầu.

### 7.2 Dữ liệu cần tạo

#### Users

| Email | Password | Role | Mô tả |
|-------|----------|------|--------|
| admin@gmail.com | 12345678 | admin | Quản trị viên |
| staff@gmail.com | 12345678 | staff | Nhân viên |
| admin@qlnh.com | 123456 | admin | Admin phụ |
| customer1@gmail.com | 12345678 | client | Khách 1 |
| customer2@gmail.com | 12345678 | client | Khách 2 |
| customer3@gmail.com | 12345678 | client | Khách 3 |

**Lưu ý:** Password cần BCrypt encode trước khi lưu.

**Đặc biệt:** User có role = "admin" và "staff" cần `emailVerified = true`.

#### Tables (7 bàn)

| Name | Capacity | Status | Location |
|------|----------|--------|----------|
| Bàn A1 | 2 | available | Tầng 1 - Cửa sổ |
| Bàn A2 | 2 | occupied | Tầng 1 - Cửa sổ |
| Bàn B1 | 4 | available | Tầng 1 - Giữa |
| Bàn B2 | 4 | reserved | Tầng 1 - Giữa |
| Bàn C1 | 6 | available | Tầng 2 - Cửa sổ |
| Bàn C2 | 8 | reserved | Tầng 2 - Ban công |
| Bàn VIP1 | 10 | available | Tầng 3 - Phòng riêng |

> **Lưu ý:** Bàn B2, C2 để `status = reserved` để demo trạng thái đã có người đặt trước.

#### Foods (10 món)

| Name | Price | Status | MealType |
|------|-------|--------|----------|
| Cà phê đen | 29000 | available | breakfast |
| Cà phê sữa | 35000 | available | breakfast |
| Trà sen vàng | 45000 | available | lunch |
| Bánh flan | 25000 | unavailable | dessert |
| Salad trộn | 65000 | available | lunch |
| Mì Ý sốt bò bằm | 85000 | available | lunch |
| Gà nướng | 250000 | available | dinner |
| Bò bít tết | 180000 | unavailable | dinner |
| Bánh pizza hải sản | 150000 | available | dinner |
| Nước cam tươi | 35000 | available | breakfast |

**Lưu ý:** `createdBy` = admin user (id=1). `createdAt` = NOW.

#### Combos (3 combo)

| Name | Price | Description | Status |
|------|-------|------------|--------|
| Combo cặp đôi | 250000 | Combo dành cho 2 người | available |
| Combo gia đình | 450000 | Combo dành cho 4 người | available |
| Combo tiệc nhỏ | 850000 | Combo dành cho 8 người | available |

#### ComboFoods

| Combo | Food | Quantity |
|-------|------|----------|
| Combo cặp đôi | Mì Ý sốt bò bằm | 2 |
| Combo cặp đôi | Bánh flan | 2 |
| Combo cặp đôi | Cà phê sữa | 2 |
| Combo gia đình | Bò bít tết | 2 |
| Combo gia đình | Bánh pizza hải sản | 2 |
| Combo gia đình | Bánh flan | 4 |
| Combo gia đình | Nước cam tươi | 4 |
| Combo tiệc nhỏ | Gà nướng | 2 |
| Combo tiệc nhỏ | Bò bít tết | 3 |
| Combo tiệc nhỏ | Bánh pizza hải sản | 3 |
| Combo tiệc nhỏ | Salad trộn | 4 |
| Combo tiệc nhỏ | Nước cam tươi | 8 |

---

### 7.3 Code mẫu DataLoader

```java
@PostConstruct
public void loadData() {
    if (userRepository.count() == 0) {
        // 1. Users
        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@gmail.com");
        admin.setPhone("0901234567");
        admin.setPassword(passwordEncoder.encode("12345678"));
        admin.setRole("admin");
        admin.setEmailVerified(true);
        admin.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        admin = userRepository.save(admin);

        // 2. Staff
        User staff = new User();
        staff.setName("Staff User");
        staff.setEmail("staff@gmail.com");
        staff.setPhone("0909876543");
        staff.setPassword(passwordEncoder.encode("12345678"));
        staff.setRole("staff");
        staff.setEmailVerified(true);
        staff.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        userRepository.save(staff);

        // 3. Tables
        Table t1 = new Table();
        t1.setName("Bàn A1");
        t1.setCapacity(2);
        t1.setStatus("available");
        t1.setLocation("Tầng 1 - Cửa sổ");
        tableRepository.save(t1);

        // ... (7 tables)

        // 4. Foods (với createdBy = admin, createdAt = NOW)

        // 5. Combos

        // 6. ComboFoods

        log.info("Initial data loaded successfully.");
    }
}
```

---

## 8. TÀI LIỆU HELPER CLASSES

### 8.1 CurrentUserResolver

```java
@Component
public class CurrentUserResolver {
    public User resolve() {
        // Lấy Authentication từ SecurityContext
        // Tìm user theo email
        // Trả về User entity
    }
}
```

**Sử dụng:** Trong controller, khi cần biết user hiện tại là ai.
```java
User currentUser = currentUserResolver.resolve();
food.setCreatedBy(currentUser);
```

### 8.2 ReservationPayloadResolver

```java
@Component
public class ReservationPayloadResolver {
    // 1. resolveUserInfo: Nếu đã login → tự điền name/email/phone
    // 2. resolveDatetime: Tách reservationAt → date + time
    // 3. resolveOrderDetails: Gán mặc định cho orderDetails
}
```

### 8.3 ReservationRequestValidator

```java
@Component
public class ReservationRequestValidator {
    public ResponseEntity validate(CreateReservationRequest request) {
        // 1. Tất cả trường bắt buộc phải có
        // 2. Giờ phải trong khoảng 06:00 - 22:00
    }
}
```

---

## 9. LUỒNG NGHIỆP VỤ CHÍNH

### 9.1 Luồng: Khách đặt bàn

```
Client                    Server                      Database
  |                          |                            |
  |-- POST /reservations --→|                            |
  |                          |-- Validate ──────────────→|
  |                          |-- Check inventory (Redis)-→|
  |                          |-- Enqueue (Redis) ───────→|
  |←── 202 Accepted ─────────|                            |
  |   (orderId)              |                            |
  |                          |-- Async: Process queue ───→|
  |                          |   1. Find user/create user  |
  |                          |   2. Find available table   |
  |                          |   3. Calculate total price  |
  |                          |   4. Save reservation ────→|
  |                          |-- SSE broadcast ──────────→|
  |←── SSE: new_reservation ──|                            |
```

### 9.2 Luồng: Đăng ký + Xác thực OTP

```
Client                    Server
  |                          |
  |-- POST /register ───────→|
  |   {name, email, phone,    |
  |    password}             |
  |                          |-- Generate OTP (6 số)
  |                          |-- Save user (emailVerified=false)
  |                          |-- Send OTP email (Async)
  |←── 200 OK ───────────────|
  |   "Kiểm tra email"        |
  |                          |
  |-- POST /verify-otp ────→|
  |   {email, otp}           |
  |                          |-- Verify OTP
  |                          |-- emailVerified=true
  |←── 200 OK ───────────────|
  |   "Xác nhận thành công"  |
```

### 9.3 Luồng: Admin đặt bàn thủ công

```
Admin                     Server                      Database
  |                          |                            |
  |-- POST /reservations ──→|                            |
  |   (admin tạo đơn)       |-- Create reservation ────→|
  |                          |   (giống trên nhưng sync)  |
  |                          |-- Return success ─────────→|
  |←── 200 OK ───────────────|                            |
```

---

## 10. CÔNG NGHỆ SỬ DỤNG

| Công nghệ | Version | Mục đích |
|-----------|---------|-----------|
| Java | 17 | Ngôn ngữ lập trình |
| Spring Boot | 3.2.4 | Framework |
| Spring Data JPA | - | ORM, Repository pattern |
| Spring Security | - | Authentication/Authorization |
| Spring Mail | - | Gửi email |
| MySQL | 8.x | Cơ sở dữ liệu |
| Lombok | 1.18.30 | Giảm boilerplate |
| JWT (jjwt) | 0.11.5 | Token-based auth |
| BCrypt | - | Password hashing |
| ModelMapper | 3.1.0 | Object mapping |
| Maven | - | Build tool |

---

## PHỤ LỤC

### A. Cách debug BCrypt password

```java
// Tạo hash cho password mới
String hash = passwordEncoder.encode("12345678");
System.out.println(hash);
// Output: $2a$10$...

// Verify password
boolean matches = passwordEncoder.matches("12345678", hash);
```

### B. Cách tạo token JWT test

```java
String token = jwtTokenProvider.generateTokenFromEmail("admin@gmail.com");
System.out.println(token);
```

### C. API Response format chuẩn

```java
// Thành công có data
return ResponseEntity.ok(ApiResponse.success("Thành công", dataObject));

// Thành công không có data
return ResponseEntity.ok(ApiResponse.success("Thành công"));

// Lỗi
return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi ở đây"));

// Lỗi 401
return ResponseEntity.status(401).body(ApiResponse.error("Không có quyền"));

// Lỗi validation
return ResponseEntity.badRequest().body(
    ApiResponse.error("Validation failed", errorsMap));
```

### D. Cách test API

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"12345678"}'

# Tạo reservation (với token)
curl -X POST http://localhost:8080/api/v1/admin/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"name":"Khách A","email":"khach@email.com","phone":"0901234567","date":"2026-05-30","time":"19:00","numberOfPeople":2,"orderDetails":"{\"Mì Ý\":1}","orderType":"food"}'
```

---

## 11. HƯỚNG DẪN THỰC HÀNH

### 11.1 Chuẩn bị môi trường

#### 11.1.1 Cài đặt Java 17

```powershell
# Kiểm tra phiên bản Java hiện tại
java -version

# Nếu chưa có Java 17, cài đặt:
# Windows: Download JDK 17 từ https://adoptium.net/
# macOS: brew install openjdk@17
# Linux: sudo apt install openjdk-17-jdk
```

#### 11.1.2 Cài đặt MySQL

```powershell
# Kiểm tra MySQL
mysql --version

# Hoặc dùng Docker:
docker run --name mysql-qlnh -e MYSQL_ROOT_PASSWORD=rootpass -e MYSQL_DATABASE=qlnh_db -e MYSQL_USER=qlnh_user -e MYSQL_PASSWORD=qlnh_password_456 -p 3309:3306 -d mysql:8

# Tạo database (nếu chưa có)
docker exec -it mysql-qlnh mysql -uroot -prootpass -e "CREATE DATABASE IF NOT EXISTS qlnh_db;"
docker exec -it mysql-qlnh mysql -uroot -prootpass -e "CREATE USER IF NOT EXISTS 'qlnh_user'@'%' IDENTIFIED BY 'qlnh_password_456';"
docker exec -it mysql-qlnh mysql -uroot -prootpass -e "GRANT ALL PRIVILEGES ON qlnh_db.* TO 'qlnh_user'@'%';"
docker exec -it mysql-qlnh mysql -uroot -prootpass -e "FLUSH PRIVILEGES;"
```

**Lưu ý quan trọng:** Port trong `application.properties` là `3309`, không phải `3306`. Nếu dùng MySQL cục bộ không qua Docker, hãy đổi thành `3306`.

#### 11.1.3 Cài đặt Maven

```powershell
# Kiểm tra Maven
mvn -version

# Nếu chưa có, tải Maven:
# Windows: https://maven.apache.org/download.cgi
# macOS: brew install maven
# Linux: sudo apt install maven
```

#### 11.1.4 Cài đặt IDE

**IntelliJ IDEA (Khuyến nghị):**
1. Tải IntelliJ IDEA Community/Ultimate từ https://jetbrains.com/idea
2. Import project: File → Open → Chọn thư mục `reservation_system`
3. Maven sẽ tự động import dependencies

**VS Code:**
1. Cài extensions: "Extension Pack for Java", "Spring Boot Extension Pack"
2. Mở thư mục project
3. File → Open Folder → Chọn `reservation_system`

### 11.2 Tạo nhánh Git cho từng feature

#### Nguyên tắc

```
┌─────────────────────────────────────────────────────────┐
│  MỖI METHOD / FEATURE = 1 NHÁNH GIT RIÊNG                │
│                                                          │
│  main ──────── feature/user-register ──── PR ──── main   │
│               feature/table-crud                           │
│               feature/food-crud                           │
│               feature/reservation-create                   │
│               ...                                          │
└─────────────────────────────────────────────────────────┘
```

#### 11.2.1 Quy tắc đặt tên nhánh

| Tiền tố | Khi nào dùng | Ví dụ |
|---------|---------------|--------|
| `feature/` | Code feature mới | `feature/user-register` |
| `fix/` | Sửa bug | `fix/delete-cascade` |
| `docs/` | Viết tài liệu | `docs/update-guide` |
| `refactor/` | Cải thiện code | `refactor/validation` |

#### 11.2.2 Workflow tạo nhánh cho method đầu tiên

**Bước 1: Đảm bảo nhánh `main` cập nhật**

```bash
# 1. Checkout về main
git checkout main

# 2. Pull code mới nhất
git pull origin main

# 3. Kiểm tra
git status
```

**Bước 2: Tạo nhánh mới cho feature đầu tiên**

```bash
# Tạo nhánh cho method registerClient
git checkout -b feature/user-register
```

**Bước 3: Xác nhận đang ở nhánh đúng**

```bash
git branch --show-current
# Output: feature/user-register
```

**Bước 4: Code method `registerClient` trong `UserService.java`**

Đọc DEV_GUIDE.md phần 5.1.1, copy logic vào file.

**Bước 5: Commit khi hoàn thành**

```bash
# Xem thay đổi
git status
git diff src/main/java/com/example/qlnh/services/UserService.java

# Stage file đã sửa
git add src/main/java/com/example/qlnh/services/UserService.java

# Commit với message rõ ràng
git commit -m "feat(user): implement registerClient method in UserService

- Add email duplicate check
- Generate 6-digit OTP
- Set OTP expiry to 15 minutes
- Send OTP email asynchronously
- Set emailVerified=false for new users"

# Push lên remote
git push -u origin feature/user-register
```

**Bước 6: Tạo Pull Request (PR)**

```bash
# Qua GitHub:
# 1. Vào https://github.com/phongdh04/reservation_system
# 2. Click "Compare & pull request"
# 3. Chọn base: main, compare: feature/user-register
# 4. Viết mô tả PR
# 5. Click "Create pull request"
```

**Bước 7: Merge PR sau khi review**

```bash
# Sau khi PR được approve trên GitHub:
# 1. Click "Merge pull request"
# 2. "Confirm merge"
# 3. Quay lại local, checkout main và pull
git checkout main
git pull origin main
```

**Bước 8: Xóa nhánh đã merge (optional)**

```bash
# Xóa nhánh local (sau khi đã merge)
git branch -d feature/user-register

# Xóa nhánh remote (sau khi đã merge)
git push origin --delete feature/user-register
```

#### 11.2.3 Tạo nhánh cho method tiếp theo

```bash
# Luôn bắt đầu từ main CẬP NHẬT NHẤT
git checkout main
git pull origin main

# Tạo nhánh mới cho method tiếp theo
git checkout -b feature/user-verify-otp

# Code xong → commit → push → tạo PR → merge
```

#### 11.2.4 Template commit message chuẩn

```
<type>(<scope>): <short description>

[optional body]

[optional footer]

# Ví dụ:
feat(user): implement registerClient method
fix(table): resolve soft delete cascade issue
docs(api): update reservation endpoint documentation
refactor(service): simplify createReservation logic
```

| Type | Mục đích |
|------|--------|
| `feat` | Feature mới |
| `fix` | Sửa bug |
| `docs` | Tài liệu |
| `refactor` | Cấu trúc lại code |
| `test` | Thêm test |
| `chore` | Công việc nhỏ (format, import) |

### 11.3 Chạy ứng dụng

#### 11.3.1 Chạy bằng Maven (Terminal/PowerShell)

```powershell
# Di chuyển vào thư mục project
cd C:\Users\phong\reservation_system

# Chạy ứng dụng
mvn spring-boot:run

# Hoặc chạy với profile cụ thể
mvn spring-boot:run -Dspring.profiles.active=dev

# Nếu gặp lỗi JAVA_HOME, set trước:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"
mvn spring-boot:run
```

**Thành công khi thấy:**
```
Started QlnhApplication in X.XXX seconds
```

**Lỗi thường gặp khi chạy:**

| Lỗi | Nguyên nhân | Cách sửa |
|------|------------|----------|
| `Connection refused: localhost:3309` | MySQL chưa chạy | Start MySQL / kiểm tra Docker container |
| `Access denied for user 'qlnh_user'` | Sai password | Kiểm tra `application.properties` |
| `JAVA_HOME not found` | Java chưa cài | Cài JDK 17 |
| `Unable to find valid certification path` | Lỗi SSL | Thêm `useSSL=false` vào connection URL |

#### 11.3.2 Chạy bằng IntelliJ IDEA

1. Mở project → File → Project Structure → SDK → Chọn Java 17
2. Run → Edit Configurations → "+" → Spring Boot
3. Chọn class: `QlnhApplication`
4. Click Run (nút tam giác xanh)
5. Console sẽ hiển thị logs

#### 11.3.3 Chạy bằng JAR file

```powershell
# Build JAR
mvn clean package -DskipTests

# Chạy JAR
java -jar target/qlnh-0.0.1-SNAPSHOT.jar
```

### 11.4 Kiểm thử API

#### 11.4.1 Công cụ kiểm thử

| Công cụ | Mô tả | Link |
|---------|--------|------|
| **Postman** (Khuyến nghị) | HTTP client mạnh mẽ | https://postman.com |
| **Thunder Client** (VS Code) | Plugin HTTP client cho VS Code | Extension trong VS Code |
| **curl** | Gọi API từ terminal | Có sẵn trong Windows/macOS/Linux |
| **Insomnia** | HTTP client nhẹ | https://insomnia.rest |

#### 11.4.2 Cách import Postman Collection

Tạo file `qlnh-api.postman_collection.json` trong project root:

```json
{
  "info": {
    "name": "QLNH Restaurant API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {"mode": "raw", "raw": "{\"email\":\"admin@gmail.com\",\"password\":\"12345678\"}"},
            "url": {"raw": "http://localhost:8080/api/v1/auth/login", "protocol": "http", "host": ["localhost:8080"], "path": ["api", "v1", "auth", "login"]}
          }
        }
      ]
    }
  ]
}
```

#### 11.4.3 Test Auth API - Đăng nhập

**Endpoint:** `POST http://localhost:8080/api/v1/auth/login`

**Request Body (Postman/Thunder Client):**
```json
{
  "email": "admin@gmail.com",
  "password": "12345678"
}
```

**Kết quả thành công:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBnbWFpbC5jb20iLCJpYXQiOjE3NDkz...",
    "email": "admin@gmail.com",
    "role": "admin",
    "name": "Admin User"
  },
  "timestamp": "2026-05-27T23:50:00"
}
```

**Cách copy token để test các API khác:**

1. Copy giá trị `token` từ response
2. Trong Postman: Tab "Authorization" → Type "Bearer Token" → Paste token
3. Tất cả API admin sẽ tự động gửi kèm token

#### 11.4.4 Test CRUD API với curl

**Đăng nhập + Lấy token:**

```powershell
# Bước 1: Login và lưu token vào biến
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"admin@gmail.com","password":"12345678"}'

$token = $response.data.token
Write-Host "Token: $token"
```

**Xem danh sách users:**

```powershell
# Bước 2: Gọi API với token
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/admin/users" `
  -Method GET `
  -Headers @{ "Authorization" = "Bearer $token" } `
  -ContentType "application/json"
```

**Tạo user mới:**

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/admin/users" `
  -Method POST `
  -Headers @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" } `
  -Body '{
    "name": "Test Staff",
    "email": "staff@qlnh.com",
    "phone": "0909999999",
    "role": "staff",
    "password": "12345678",
    "confirmPassword": "12345678"
  }'
```

#### 11.4.5 Cách kiểm tra DataLoader đã chạy chưa

Sau khi chạy `mvn spring-boot:run` lần đầu, kiểm tra:

```sql
-- Kiểm tra users đã được tạo chưa
SELECT * FROM users;

-- Kiểm tra bàn đã được tạo chưa
SELECT * FROM tables;

-- Kiểm tra món ăn đã được tạo chưa
SELECT * FROM foods;
```

Nếu có dữ liệu → DataLoader đã chạy thành công.

**Lỗi DataLoader không chạy:**

Nếu chạy lần đầu mà không có dữ liệu, kiểm tra:
1. Database connection trong `application.properties` có đúng không?
2. `userRepository.count() == 0` trong DataLoader đã được implement chưa (hiện là TODO)
3. MySQL có đang chạy không?

#### 11.4.6 Log output khi chạy

Khi chạy thành công, terminal sẽ hiển thị:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/ /_/_/
 :: Spring Boot ::                (v3.2.4)

2026-05-27 23:50:00 [main] INFO  com.example.qlnh.QlnhApplication - Starting QlnhApplication...
2026-05-27 23:50:01 [main] INFO  com.example.qlnh.DataLoader - Loading initial data...
2026-05-27 23:50:02 [main] INFO  com.example.qlnh.DataLoader - Initial data loaded successfully.
2026-05-27 23:50:02 [main] INFO  com.example.qlnh.QlnhApplication - Started QlnhApplication in 3.456 seconds
```

### 11.5 Lộ trình code theo thứ tự

#### Thứ tự bắt buộc phải tuân theo

```
Bước 1: DataLoader          ← Tạo dữ liệu mẫu để test
Bước 2: UserService (5 method)  ← User là nền tảng cho mọi thứ
Bước 3: AuthController       ← Có auth mới test được API
Bước 4: UserController       ← CRUD user
Bước 5: TableService + Controller   ← Bàn độc lập nhất
Bước 6: FoodService + Controller    ← Món ăn
Bước 7: ComboService + Controller   ← Combo phụ thuộc Food
Bước 8: ReservationService + Controller ← Phụ thuộc User + Table + Food + Combo
Bước 9: ReviewService + Controller    ← Phụ thuộc User
Bước 10: EmailService              ← Gửi email
Bước 11: Client APIs              ← APIs public
```

#### Chi tiết từng bước

**Bước 1: DataLoader** (5 method cần viết trong UserService)
- Implement `registerClient` → DataLoader cần tạo user
- Implement `verifyOtp` → DataLoader cần set verificationToken
- Implement `verifyEmail` → DataLoader cần set verificationToken
- Implement `deleteUser` → Cascade delete
- Implement `comparePassword`, `checkEmailExist`, `checkPhoneExist` → Đã có sẵn

**Bước 2: AuthController** (5 endpoints)
- `POST /login` → Gọi registerClient
- `POST /register` → Gọi verifyOtp
- `POST /verify-otp` → Gọi verifyEmail
- `GET /verify-email`
- `GET /me`

**Bước 3-9:** Làm theo hướng dẫn trong DEV_GUIDE.md từng phần.

#### Cách test sau mỗi bước

| Bước | Cách test |
|------|----------|
| DataLoader | Query `SELECT * FROM users;` trong MySQL |
| Auth (login) | Gọi `POST /api/v1/auth/login` với Postman |
| User CRUD | Gọi CRUD API với token đã lấy ở bước Auth |
| Table CRUD | Gọi Table API |
| Food CRUD | Gọi Food API |
| Combo CRUD | Gọi Combo API |
| Reservation | Tạo reservation + kiểm tra DB |
| Review | Tạo review + kiểm tra DB |
| Email | Check inbox (email thật hoặc log console) |

#### Checklist trước khi commit

- [ ] Code compile không lỗi (`mvn compile`)
- [ ] Chạy được không crash (`mvn spring-boot:run`)
- [ ] API trả về đúng response format (`ApiResponse`)
- [ ] Exception được xử lý đúng (`GlobalExceptionHandler`)
- [ ] Không có `System.out.println` (dùng `log.info/error/warn`)
- [ ] Commit message đúng format
- [ ] Push lên remote

#### Mẹo debug khi API không hoạt động

```
1. Kiểm tra MySQL có chạy không?
   → mysql -uqlnh_user -pqlnh_password_456 -h localhost -P 3309 qlnh_db

2. Kiểm tra application.properties có đúng không?
   → So sánh với config MySQL thực tế

3. Kiểm tra token JWT có đúng không?
   → Copy token từ /login → https://jwt.io để decode

4. Kiểm tra Security Config có block API không?
   → Xem file SecurityConfig.java - endpoint có được permitAll không?

5. Bật log SQL để xem query:
   → Trong application.properties:
   spring.jpa.show-sql=true
   logging.level.org.hibernate.SQL=DEBUG

6. Restart app sau khi thay đổi code:
   → Ctrl+C rồi chạy lại, hoặc dùng Spring Boot DevTools auto-restart
```

