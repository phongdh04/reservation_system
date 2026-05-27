# FEATURE: OTP Verification

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Luồng kỹ thuật](#2-luồng-kỹ-thuật)
3. [Sơ đồ Sequence](#3-sơ-đồ-sequence)
4. [Chi tiết từng bước](#4-chi-tiết-từng-bước)
5. [Tài liệu liên quan](#5-tài-liệu-liên-quan)

---

## 1. Tổng quan

### Mô tả
Khách hàng sau khi đăng ký → nhận mã OTP 6 số qua email → nhập OTP vào app → hệ thống kiểm tra OTP và kích hoạt tài khoản → khách có thể đăng nhập.

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
AuthApiController.java          ← Endpoint entry point (/verify-otp)
    │
    ▼
UserService.java               ← Business logic (verifyOtp)
    │
    ├──► UserRepository.java       ← UPDATE user: emailVerified=true
    │
    └── (no email)
```

### Khi nào verify OTP?

```
[ĐĂNG KÝ] ────(user created, emailVerified=false)───→ [EMAIL OTP]
                                                      │
                                                      ▼
                                              Khách nhận OTP
                                                      │
                                                      ▼
[ĐĂNG NHẬP] ─(emailVerified=false)──→ BỊ CHẶN ──→ [VERIFY OTP]
                                                      │
                                                      ▼
                                    (emailVerified=true)
                                                      │
                                                      ▼
                                        [ĐĂNG NHẬP THÀNH CÔNG]
```

---

## 2. Luồng kỹ thuật

### 2.1 Request đi qua các file

```
HTTP POST /api/v1/auth/verify-otp
│
├─[1] JwtAuthFilter.java          (Security filter)
│   ├─ skip: /api/v1/auth/** → permitAll
│   └─ cho request đi qua
│
├─[2] AuthApiController.java       (@RestController)
│   ├─ @PostMapping("/verify-otp")
│   ├─ @RequestBody Map<String, String> body
│   ├─ Extract: email, otp
│   ├─ Validate input
│   └─ Gọi: userService.verifyOtp(email, otp)
│
├─[3] UserService.java            (@Service)
│   ├─ verifyOtp(email, otp)
│   ├─ 1. findByEmail(email) → UserRepository
│   ├─ 2. Kiểm tra user != null
│   ├─ 3. Kiểm tra emailVerified == false
│   ├─ 4. Kiểm tra verificationToken == otp
│   ├─ 5. Kiểm tra otpExpiry > NOW
│   ├─ 6. user.setEmailVerified(true)
│   ├─ 7. user.setVerificationToken(null)
│   ├─ 8. user.setOtpExpiry(null)
│   └─ 9. save(user) → UserRepository (UPDATE)
│
├─[4] UserRepository.java        (JPA Repository)
│   └─ save(User user) → UPDATE users SET email_verified=true WHERE email=?
│
└─[5] ApiResponse.java            (Wrapper response)
    └─ return: {"success":true, "message":"...", "data":{"email":"...", "name":"..."}}
```

### 2.2 Chi tiết từng file

#### [1] `JwtAuthFilter.java` - Security Filter

```java
// File: src/main/java/com/example/qlnh/filter/JwtAuthFilter.java
// Vai trò: Intercepts mọi request HTTP

// Bước 1: Extract JWT từ header "Authorization: Bearer <token>"
String bearerToken = request.getHeader("Authorization");
if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
    jwt = bearerToken.substring(7);
}

// Bước 2: Validate token
if (jwt != null && tokenProvider.validateToken(jwt)) {
    // Set authentication vào SecurityContext
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
    authentication.setDetails(
        new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
}

// Bước 3: QUAN TRỌNG - Public endpoints bypass validation
// Trong SecurityConfig.java:
// .requestMatchers("/api/v1/auth/**").permitAll()
// → JwtAuthFilter vẫn chạy nhưng skip validation cho /auth/*
```

**Đặc điểm kỹ thuật:**
- Filter này chạy cho MỌI request (bao gồm cả verify-otp)
- Nhưng `/api/v1/auth/**` không yêu cầu JWT → skip validation
- Dùng `OncePerRequestFilter` (mỗi request chỉ chạy 1 lần)

#### [2] `AuthApiController.java` - Controller

```java
// File: src/main/java/com/example/qlnh/controllers/api/AuthApiController.java

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final IUserService userService;

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        // 1. Extract params
        String email = body.getOrDefault("email", "").trim();
        String otp = body.getOrDefault("otp", "").trim();

        // 2. Validate input
        if (email.isEmpty() || otp.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Vui lòng nhập đầy đủ email và mã OTP."));
        }

        if (otp.length() != 6) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Mã OTP phải có 6 chữ số."));
        }

        // 3. Gọi Service
        try {
            User user = userService.verifyOtp(email, otp);
            return ResponseEntity.ok(ApiResponse.success(
                "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
                Map.of(
                    "email", user.getEmail(),
                    "name", user.getName()
                )));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

**HTTP Request Example:**
```
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
    "email": "nguyenvana@email.com",
    "otp": "385921"
}
```

**HTTP Response Example:**
```json
{
    "success": true,
    "message": "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
    "data": {
        "email": "nguyenvana@email.com",
        "name": "Nguyen Van A"
    },
    "timestamp": "2026-05-28T06:32:00"
}
```

#### [3] `UserService.java` - Business Logic

```java
// File: src/main/java/com/example/qlnh/services/UserService.java

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;

    @Transactional
    public User verifyOtp(String email, String otp) {
        // ═══════════════════════════════════════════════════════
        // BƯỚC 1: Tìm user theo email
        // ═══════════════════════════════════════════════════════
        // SELECT * FROM users WHERE email = 'nguyenvana@email.com'
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException(
                "Không tìm thấy tài khoản với email này.");
        }

        // ═══════════════════════════════════════════════════════
        // BƯỚC 2: Kiểm tra đã xác thực chưa
        // ═══════════════════════════════════════════════════════
        // Nếu đã xác thực rồi → không cho verify lại
        // Trường hợp: User đã verify thành công trước đó
        // → Không cần gửi lại OTP, chỉ thông báo đã xác nhận rồi
        if (user.isEmailVerified()) {
            throw new BusinessValidationException(
                "Tài khoản đã được xác nhận trước đó. Vui lòng đăng nhập.");
        }

        // ═══════════════════════════════════════════════════════
        // BƯỚC 3: Kiểm tra OTP có khớp không
        // ═══════════════════════════════════════════════════════
        // So sánh chuỗi: verification_token (DB) == otp (input)
        // Ví dụ: DB lưu "385921" → user nhập "385921" → KHỚP ✓
        //         DB lưu "385921" → user nhập "123456" → KHÔNG KHỚP ✗
        if (user.getVerificationToken() == null ||
            !user.getVerificationToken().equals(otp)) {
            throw new BusinessValidationException(
                "Mã OTP không đúng. Vui lòng kiểm tra lại email!");
        }

        // ═══════════════════════════════════════════════════════
        // BƯỚC 4: Kiểm tra OTP còn hạn không
        // ═══════════════════════════════════════════════════════
        // otpExpiry = thời điểm hết hạn (được set khi đăng ký = NOW + 15 phút)
        // NOW = thời điểm hiện tại (khi user verify)
        //
        // Scenario 1: User verify sau 10 phút
        //   otpExpiry = 06:46, NOW = 06:40 → 06:40 < 06:46 → CÒN HẠN ✓
        //
        // Scenario 2: User verify sau 20 phút (quá hạn)
        //   otpExpiry = 06:46, NOW = 07:00 → 07:00 > 06:46 → HẾT HẠN ✗
        if (user.getOtpExpiry() == null ||
            user.getOtpExpiry().before(new Timestamp(System.currentTimeMillis()))) {
            throw new BusinessValidationException(
                "Mã OTP đã hết hạn. Vui lòng đăng ký lại để nhận mã mới.");
        }

        // ═══════════════════════════════════════════════════════
        // BƯỚC 5: Xác thực thành công - cập nhật user
        // ═══════════════════════════════════════════════════════
        // Các trường cần cập nhật:
        user.setEmailVerified(true);         // true = đã xác thực email
        user.setVerificationToken(null);    // Xóa OTP (không dùng nữa)
        user.setOtpExpiry(null);            // Xóa expiry (không dùng nữa)

        // ═══════════════════════════════════════════════════════
        // BƯỚC 6: Lưu vào DB
        // ═══════════════════════════════════════════════════════
        // UPDATE users
        // SET email_verified = true,
        //     verification_token = NULL,
        //     otp_expiry = NULL
        // WHERE id = ?
        User updatedUser = userRepository.save(user);

        return updatedUser;
    }
}
```

**Database state trước khi verify:**
```sql
SELECT id, name, email, email_verified, verification_token, otp_expiry
FROM users WHERE email = 'nguyenvana@email.com';

+----+--------------+------------------------+---------+-------------------+--------------------+
| id | name        | email                  | verified| verification_token| otp_expiry          |
+----+--------------+------------------------+---------+-------------------+--------------------+
| 10 | Nguyen Van A | nguyenvana@email.com   | false   | 385921            | 2026-05-28 06:46:00|
+----+--------------+------------------------+---------+-------------------+--------------------+
```

**Database state sau khi verify:**
```sql
SELECT id, name, email, email_verified, verification_token, otp_expiry
FROM users WHERE email = 'nguyenvana@email.com';

+----+--------------+------------------------+---------+-------------------+--------------------+
| id | name        | email                  | verified| verification_token| otp_expiry          |
+----+--------------+------------------------+---------+-------------------+--------------------+
| 10 | Nguyen Van A | nguyenvana@email.com   | true    | NULL              | NULL               |
+----+--------------+------------------------+---------+-------------------+--------------------+
```

#### [4] `UserRepository.java` - Data Access

```java
// File: src/main/java/com/example/qlnh/repositories/UserRepository.java

public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository đã có sẵn:
    // save(User) → INSERT (new) / UPDATE (existing)
    // findById(Long) → SELECT ... WHERE id = ?
    // findAll() → SELECT * FROM users
    // deleteById(Long) → DELETE ...

    // Custom query - do developer định nghĩa:
    User findByEmail(String email);
    // → Spring tự generate: SELECT * FROM users WHERE email = ?

    User findByEmailOrPhone(String email, String phone);
    // → SELECT * FROM users WHERE email = ? OR phone = ?

    List<User> findByRole(String role);
    // → SELECT * FROM users WHERE role = ?

    Long checkEmailExist(String email, Long id);
    // → SELECT COUNT(*) FROM users WHERE email = ? AND id != ?
    // Dùng khi UPDATE user - loại trừ chính user đang update
}
```

#### [5] `ApiResponse.java` - Response Wrapper

```java
// File: src/main/java/com/example/qlnh/dto/response/ApiResponse.java

@Data @Builder
@JsonInclude(Include.NON_NULL)      // Không serialize null fields
public class ApiResponse<T> {
    private boolean success;        // true = thành công, false = lỗi
    private String message;         // Thông báo cho user
    private T data;                // Data (nếu có)
    private LocalDateTime timestamp; // Thời điểm response

    // Factory methods - cách tạo response chuẩn:
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(now())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .timestamp(now())
            .build();
    }
}
```

---

## 3. Sơ đồ Sequence

### 3.1 Verify OTP Flow

```
Client          Controller        Service         Repository
  |                |                |                |
  |                |                |                |
  |---POST /verify-otp (email, otp) →|               |
  |                |                |                |
  |                |--verifyOtp()→|                |
  |                |               |                |
  |                |  ①findByEmail(email)→         |
  |                |               |←──────────────| (user found)
  |                |               |                |
  |                |               | ②Check: emailVerified==false?
  |                |               |   └─ false ✓ → continue
  |                |               |                |
  |                |               | ③Check: token==otp?
  |                |               |   └─ "385921"=="385921" ✓
  |                |               |                |
  |                |               | ④Check: expiry > NOW?
  |                |               |   └─ 06:46 > 06:32 ✓
  |                |               |                |
  |                |               | ⑤user.setEmailVerified(true)
  |                |               | ⑥user.setVerificationToken(null)
  |                |               | ⑦user.setOtpExpiry(null)
  |                |               |                |
  |                |               | ⑧save(user)→  |
  |                |               |   UPDATE users SET...   |
  |                |               |←──────────────| (updated)
  |                |               |                |
  |                |←User (verified)               |
  |                |                |                |
  |←── 200 OK ─────|                |                |
  |                |                |                |
  {"success":true, "message":"Xác nhận thành công!"}
```

### 3.2 Trường hợp lỗi

#### Trường hợp 1: Email không tồn tại

```
Client          Controller        Service
  |                |                |
  |--POST /verify-otp →|            |
  |                |--verifyOtp()→|
  |                |  ①findByEmail(email)→|
  |                |←─── null ───────|
  |                |                |
  |                | THROW ResourceNotFoundException
  |                |←──────────────|
  |←── 400 Bad Request ───|
  {"success":false, "message":"Không tìm thấy tài khoản..."}
```

#### Trường hợp 2: OTP không khớp

```
Client          Controller        Service
  |                |                |
  |--POST /verify-otp →|            |
  |                |--verifyOtp()→|
  |                |  ①findByEmail(email)→|
  |                |←─── user ──────|
  |                |                |
  |                |  ②emailVerified=false ✓
  |                |                |
  |                |  ③token=="385921" vs otp="000000"
  |                |     └─ KHÔNG KHỚP ✗
  |                |                |
  |                | THROW BusinessValidationException("Mã OTP không đúng...")
  |                |←──────────────|
  |←── 400 Bad Request ───|
  {"success":false, "message":"Mã OTP không đúng. Vui lòng kiểm tra lại email!"}
```

#### Trường hợp 3: OTP hết hạn

```
Client          Controller        Service
  |                |                |
  |--POST /verify-otp →|            |
  |                |--verifyOtp()→|
  |                |  ①findByEmail(email)→|
  |                |←─── user ──────|
  |                |                |
  |                |  ②emailVerified=false ✓
  |                |  ③token==otp ✓
  |                |                |
  |                |  ④expiry=06:46 vs NOW=07:00
  |                |     └─ 07:00 > 06:46 → HẾT HẠN ✗
  |                |                |
  |                | THROW BusinessValidationException("Mã OTP đã hết hạn...")
  |                |←──────────────|
  |←── 400 Bad Request ───|
  {"success":false, "message":"Mã OTP đã hết hạn. Vui lòng đăng ký lại..."}
```

#### Trường hợp 4: Tài khoản đã xác thực

```
Client          Controller        Service
  |                |                |
  |--POST /verify-otp →|            |
  |                |--verifyOtp()→|
  |                |  ①findByEmail(email)→|
  |                |←─── user ──────|
  |                |                |
  |                |  ②emailVerified=true
  |                |     └─ ĐÃ XÁC THỰC RỒI ✗
  |                |                |
  |                | THROW BusinessValidationException("Tài khoản đã được xác nhận...")
  |                |←──────────────|
  |←── 400 Bad Request ───|
  {"success":false, "message":"Tài khoản đã được xác nhận trước đó. Vui lòng đăng nhập."}
```

---

## 4. Chi tiết từng bước

### Bước 1: Client gửi request verify OTP

```http
POST /api/v1/auth/verify-otp HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
    "email": "nguyenvana@email.com",
    "otp": "385921"
}
```

### Bước 2: JwtAuthFilter kiểm tra (skip vì permitAll)

```java
// JwtAuthFilter.java - doFilterInternal()
String requestURI = request.getRequestURI();
// "/api/v1/auth/verify-otp" → starts with "/api/v1/auth/"
// → SecurityConfig: .requestMatchers("/api/v1/auth/**").permitAll()
// → Không kiểm tra JWT, cho request đi qua
```

### Bước 3: AuthApiController nhận request

```java
// AuthApiController.java - verifyOtp()
@PostMapping("/verify-otp")
public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
    // Spring tự deserialize JSON → Map
    String email = body.getOrDefault("email", "").trim();
    String otp = body.getOrDefault("otp", "").trim();

    // Validate
    if (email.isEmpty() || otp.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Vui lòng nhập đầy đủ email và mã OTP."));
    }

    try {
        User user = userService.verifyOtp(email, otp);
        return ResponseEntity.ok(ApiResponse.success(
            "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
            Map.of("email", user.getEmail(), "name", user.getName())));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

### Bước 4: UserService xử lý business logic

```java
// UserService.java - verifyOtp()
@Transactional
public User verifyOtp(String email, String otp) {
    // 1. Validate email tồn tại
    User user = userRepository.findByEmail(email);
    if (user == null) {
        throw new ResourceNotFoundException(
            "Không tìm thấy tài khoản với email này.");
    }

    // 2. Validate đã xác thực chưa
    if (user.isEmailVerified()) {
        throw new BusinessValidationException(
            "Tài khoản đã được xác nhận trước đó. Vui lòng đăng nhập.");
    }

    // 3. Validate OTP khớp
    if (user.getVerificationToken() == null ||
        !user.getVerificationToken().equals(otp)) {
        throw new BusinessValidationException(
            "Mã OTP không đúng. Vui lòng kiểm tra lại email!");
    }

    // 4. Validate OTP còn hạn
    if (user.getOtpExpiry() == null ||
        user.getOtpExpiry().before(new Timestamp(System.currentTimeMillis()))) {
        throw new BusinessValidationException(
            "Mã OTP đã hết hạn. Vui lòng đăng ký lại để nhận mã mới.");
    }

    // 5. Cập nhật user
    user.setEmailVerified(true);
    user.setVerificationToken(null);
    user.setOtpExpiry(null);

    // 6. Persist
    User updatedUser = userRepository.save(user);

    return updatedUser;
}
```

### Bước 5: Database update

```sql
-- UserRepository.save() → JPA → Hibernate → JDBC → MySQL
-- Hibernate nhận diện user đã có id=10 → tự hiểu là UPDATE

UPDATE users
SET
    email_verified = true,
    verification_token = NULL,
    otp_expiry = NULL,
    updated_at = '2026-05-28 06:32:00'
WHERE id = 10;
-- Result: 1 row updated
```

### Bước 6: Client nhận response

```json
// HTTP 200 OK
{
    "success": true,
    "message": "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
    "data": {
        "email": "nguyenvana@email.com",
        "name": "Nguyen Van A"
    },
    "timestamp": "2026-05-28T06:32:00"
}
```

---

## 5. Tài liệu liên quan

| File | Đường dẫn | Vai trò |
|------|-----------|---------|
| `AuthApiController.java` | `controllers/api/AuthApiController.java` | Nhận request, routing |
| `UserService.java` | `services/UserService.java` | Business logic |
| `UserRepository.java` | `repositories/UserRepository.java` | CRUD database |
| `ApiResponse.java` | `dto/response/ApiResponse.java` | Wrapper response |
| `User.java` | `models/entities/User.java` | Entity mapping |
| `JwtAuthFilter.java` | `filter/JwtAuthFilter.java` | Security filter |
| `SecurityConfig.java` | `config/SecurityConfig.java` | Security config (permitAll) |
| `ResourceNotFoundException.java` | `exception/ResourceNotFoundException.java` | Exception khi không tìm thấy user |
| `BusinessValidationException.java` | `exception/BusinessValidationException.java` | Exception khi OTP không hợp lệ |
| `GlobalExceptionHandler.java` | `exception/GlobalExceptionHandler.java` | Bắt exception |
| `user-registration-otp.md` | `docs/features/user-registration-otp.md` | Tài liệu đăng ký + OTP (phần 1) |
| `DEV_GUIDE.md` | `DEV_GUIDE.md` | Tài liệu tổng hợp |

## Phụ lục: So sánh Register vs Verify OTP

| Khía cạnh | Đăng ký (Register) | Xác thực OTP (Verify) |
|-----------|-------------------|----------------------|
| **Endpoint** | POST `/auth/register` | POST `/auth/verify-otp` |
| **Mục đích** | Tạo tài khoản mới | Kích hoạt tài khoản |
| **Public/Auth** | Public | Public |
| **Input chính** | name, email, phone, password | email, otp |
| **DB Operation** | INSERT (tạo user mới) | UPDATE (cập nhật user) |
| **Email** | Gửi OTP đến email | Không gửi |
| **@Async** | Có (gửi email) | Không |
| **@Transactional** | Có | Có |
| **Validation** | Email unique, password >= 6 chars | OTP khớp, chưa hết hạn |

## Phụ lục: Các trường hợp lỗi và mã HTTP

| Trường hợp | Exception | HTTP Status | Message |
|-----------|-----------|-------------|---------|
| Email không tồn tại | `ResourceNotFoundException` | 400 Bad Request | Không tìm thấy tài khoản với email này |
| Tài khoản đã xác thực | `BusinessValidationException` | 400 Bad Request | Tài khoản đã được xác nhận trước đó |
| OTP không khớp | `BusinessValidationException` | 400 Bad Request | Mã OTP không đúng |
| OTP hết hạn | `BusinessValidationException` | 400 Bad Request | Mã OTP đã hết hạn |
| Input rỗng | (validate ở Controller) | 400 Bad Request | Vui lòng nhập đầy đủ email và mã OTP |
| Lỗi server | `Exception` | 500 Internal Server Error | Xác nhận thất bại |
