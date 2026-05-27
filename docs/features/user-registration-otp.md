# FEATURE: User Registration + OTP Verification

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Luồng kỹ thuật - Đăng ký](#2-luồng-kỹ-thuật---đăng-ký)
3. [Luồng kỹ thuật - Xác thực OTP](#3-luồng-kỹ-thuật---xác-thực-otp)
4. [Sơ đồ sequence](#4-sơ-đồ-sequence)
5. [Chi tiết từng bước](#5-chi-tiết-từng-bước)
6. [Tài liệu liên quan](#6-tài-liệu-liên-quan)

---

## 1. Tổng quan

### Mô tả
Khách hàng đăng ký tài khoản mới → Hệ thống gửi mã OTP 6 số qua email → Khách nhập OTP để xác thực → Tài khoản được kích hoạt.

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
AuthApiController.java          ← Endpoint entry point
    │ POST /auth/register
    │
    ▼
UserService.java               ← Business logic
    │ registerClient()
    │
    ├──► UserRepository.java       ← Lưu user vào DB
    │
    └──► EmailService.java         ← Gửi email OTP (async)
            │
            ▼
        SMTP/Gmail             ← Email thực sự được gửi
```

### OTP Flow

```
REQUEST HTTP
    │
    ▼
AuthApiController.java          ← Endpoint: POST /auth/verify-otp
    │
    ▼
UserService.java               ← Business logic
    │ verifyOtp()
    │
    └──► UserRepository.java       ← Cập nhật user (emailVerified=true)
```

---

## 2. Luồng kỹ thuật - Đăng ký

### 2.1 Request đi qua các file

```
HTTP POST /api/v1/auth/register
│
├─[1] JwtAuthFilter.java          (Security filter - kiểm tra JWT token)
│   ├─ skip: đây là public endpoint → permitAll
│   └─ cho request đi qua
│
├─[2] AuthApiController.java       (@RestController - nhận request)
│   ├─ @PostMapping("/register")
│   ├─ @RequestBody Map<String, String> body
│   ├─ Extract: name, email, phone, password, confirmPassword
│   └─ Gọi: userService.registerClient(...)
│
├─[3] UserService.java            (@Service - business logic)
│   ├─ registerClient(name, email, phone, password)
│   ├─ 1. findByEmail(email) → UserRepository
│   ├─ 2. Tạo OTP 6 số ngẫu nhiên
│   ├─ 3. Tạo otpExpiry = NOW + 15 phút
│   ├─ 4. Tạo User entity
│   ├─ 5. passwordEncoder.encode(password) → BCrypt hash
│   ├─ 6. save(user) → UserRepository
│   └─ 7. emailService.sendOtpEmail() → EmailService (async)
│
├─[4] UserRepository.java        (JPA Repository)
│   ├─ findByEmail(String email) → SELECT * FROM users WHERE email=?
│   └─ save(User user) → INSERT INTO users (...)
│
├─[5] EmailService.java          (@Service - gửi email)
│   ├─ @Async (chạy nền, không block request)
│   ├─ JavaMailSender.createMimeMessage()
│   ├─ MimeMessageHelper (HTML email)
│   └─ mailSender.send(message)
│
└─[6] ApiResponse.java            (Wrapper response)
    └─ return: {"success":true, "message":"...", "data":{"email":"..."}}
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
}

// Bước 3: QUAN TRỌNG - Public endpoints bypass filter
// Trong SecurityConfig.java:
// .requestMatchers("/api/v1/auth/**").permitAll()
// → JwtAuthFilter vẫn chạy nhưng skip validation cho auth/*
```

**Đặc điểm kỹ thuật:**
- Filter này chạy cho MỌI request
- Nhưng `/api/v1/auth/**` không require JWT → skip validation
- Dùng `OncePerRequestFilter` (mỗi request chỉ chạy 1 lần)

#### [2] `AuthApiController.java` - Controller

```java
// File: src/main/java/com/example/qlnh/controllers/api/AuthApiController.java

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final IUserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        // 1. Extract params
        String name = body.getOrDefault("name", "").trim();
        String email = body.getOrDefault("email", "").trim();
        String phone = body.getOrDefault("phone", "").trim();
        String password = body.getOrDefault("password", "").trim();
        String confirmPassword = body.getOrDefault("confirmPassword", "").trim();

        // 2. Validate input
        if (name.isEmpty() || email.isEmpty() || password.isEmpty())
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng điền đầy đủ thông tin"));

        if (!password.equals(confirmPassword))
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu xác nhận không khớp"));

        if (password.length() < 6)
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu phải có ít nhất 6 ký tự"));

        // 3. Gọi Service
        try {
            userService.registerClient(name, email, phone, password);
            return ResponseEntity.ok(ApiResponse.success(
                "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.",
                Map.of("email", email)));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Đăng ký thất bại. Vui lòng thử lại."));
        }
    }
}
```

**HTTP Request Example:**
```
POST /api/v1/auth/register
Content-Type: application/json

{
    "name": "Nguyen Van A",
    "email": "nguyenvana@email.com",
    "phone": "0901234567",
    "password": "12345678",
    "confirmPassword": "12345678"
}
```

**HTTP Response Example:**
```json
{
    "success": true,
    "message": "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.",
    "data": {
        "email": "nguyenvana@email.com"
    },
    "timestamp": "2026-05-28T06:31:00"
}
```

#### [3] `UserService.java` - Business Logic

```java
// File: src/main/java/com/example/qlnh/services/UserService.java

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerClient(String name, String email, String phone, String password) {
        // Bước 1: Kiểm tra email đã tồn tại chưa
        // SELECT * FROM users WHERE email = 'nguyenvana@email.com'
        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            throw new DuplicateResourceException("Email đã được sử dụng: " + email);
        }

        // Bước 2: Tạo mã OTP 6 số
        // VD: "385921"
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Bước 3: Tạo thời điểm hết hạn = NOW + 15 phút
        Timestamp expiry = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));

        // Bước 4: Tạo User entity
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        // BCrypt encode: "12345678" → "$2a$10$Hd2iSSXPv5GNck5IxLtQN..."
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("client");                    // role = client (khách hàng)
        user.setEmailVerified(false);              // CHƯA xác thực
        user.setVerificationToken(otp);            // Lưu OTP để verify sau
        user.setOtpExpiry(expiry);                 // Hết hạn sau 15 phút

        // Bước 5: Lưu vào DB
        // INSERT INTO users (name, email, phone, password, role, email_verified,
        //                     verification_token, otp_expiry, created_at)
        // VALUES (...)
        User savedUser = userRepository.save(user);

        // Bước 6: Gửi email OTP (async - không block request)
        // → EmailService.sendOtpEmail() chạy ở thread riêng
        emailService.sendOtpEmail(email, name, otp);

        // Bước 7: Trả về user đã lưu
        return savedUser;
    }
}
```

**Database state sau khi đăng ký:**
```sql
SELECT id, name, email, role, email_verified, verification_token, otp_expiry, created_at
FROM users WHERE email = 'nguyenvana@email.com';

+----+--------------+------------------------+------+---------+-------------------+--------------------+---------------------+
| id | name        | email                  | role | verified| verification_token | otp_expiry          | created_at          |
+----+--------------+------------------------+------+---------+-------------------+--------------------+---------------------+
| 10 | Nguyen Van A | nguyenvana@email.com   |client| false   | 385921            | 2026-05-28 06:46:00| 2026-05-28 06:31:00|
+----+--------------+------------------------+------+---------+-------------------+--------------------+---------------------+
```

#### [4] `UserRepository.java` - Data Access

```java
// File: src/main/java/com/example/qlnh/repositories/UserRepository.java

public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository đã có sẵn:
    // save(User) → INSERT / UPDATE
    // findById(Long) → SELECT ... WHERE id = ?
    // deleteById(Long) → DELETE ...

    // Custom query - do developer định nghĩa:
    User findByEmail(String email);
    // → Spring tự generate: SELECT * FROM users WHERE email = ?

    User findByEmailOrPhone(String email, String phone);
    // → SELECT * FROM users WHERE email = ? OR phone = ?

    Long checkEmailExist(String email, Long id);
    // → SELECT COUNT(*) FROM users WHERE email = ? AND id != ?
    // Dùng để kiểm tra trùng khi UPDATE (loại trừ chính user đang update)
}
```

**Cách Spring tự generate query từ method name:**

| Method Name | SQL Generated |
|------------|--------------|
| `findByEmail` | `SELECT * FROM users WHERE email = ?` |
| `findByEmailOrPhone` | `SELECT * FROM users WHERE email = ? OR phone = ?` |
| `findByRole` | `SELECT * FROM users WHERE role = ?` |
| `findByNameContaining` | `SELECT * FROM users WHERE name LIKE '%?%'` |
| `countByRole` | `SELECT COUNT(*) FROM users WHERE role = ?` |

#### [5] `EmailService.java` - Email Sending

```java
// File: src/main/java/com/example/qlnh/services/EmailService.java

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Async                    // ← CHẠY Ở THREAD KHÁC - không block request
    @Override
    public void sendOtpEmail(String toEmail, String name, String otp) {
        try {
            // 1. Tạo MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 2. Set email properties
            helper.setFrom(fromEmail);              // Từ: noreply@example.com
            helper.setTo(toEmail);                  // Đến: nguyenvana@email.com
            helper.setSubject("Mã xác nhận OTP - Nhà Hàng");

            // 3. Tạo HTML body
            String html = buildOtpEmailHtml(name, otp);

            // 4. Set content (HTML = true)
            helper.setText(html, true);

            // 5. Gửi email thật sự
            mailSender.send(message);

            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            // Email fail không ảnh hưởng đến registration
            // Chỉ log lỗi
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String name, String otp) {
        return """
            <div style="font-family: 'Segoe UI', sans-serif;
                        max-width: 500px; margin: 0 auto;
                        padding: 2rem; background: #f8f9fc;
                        border-radius: 12px;">
                <h2 style="color: #1a1a2e; text-align: center;">
                    Xác nhận tài khoản!
                </h2>
                <p>Xin chào <strong>""" + name + """</strong>,</p>
                <p>Đây là mã OTP. Mã có hiệu lực trong <strong>15 phút</strong>:</p>
                <div style="text-align: center; margin: 2rem 0;">
                    <div style="display: inline-block;
                                background: linear-gradient(135deg,#e74a3b,#f6c23e);
                                color: #fff; padding: 1rem 3rem;
                                border-radius: 16px;
                                font-size: 2.5rem; font-weight: 900;
                                letter-spacing: 0.5rem;">
                        """ + otp + """
                    </div>
                </div>
            </div>
            """;
    }
}
```

**Email nhận được:**
```
Subject: Mã xác nhận OTP - Nhà Hàng
From: noreply@example.com
To: nguyenvana@email.com

[Xác nhận tài khoản!]
Xin chào Nguyen Van A,
Đây là mã OTP. Mã có hiệu lực trong 15 phút:

        3 8 5 9 2 1
```

#### [6] `ApiResponse.java` - Response Wrapper

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

## 3. Luồng kỹ thuật - Xác thực OTP

### 3.1 Request đi qua các file

```
HTTP POST /api/v1/auth/verify-otp
│
├─[1] JwtAuthFilter.java          (Security - permitAll → skip)
│
├─[2] AuthApiController.java       (@RestController)
│   ├─ @PostMapping("/verify-otp")
│   ├─ @RequestBody Map<String, String> body
│   ├─ Extract: email, otp
│   └─ Gọi: userService.verifyOtp(email, otp)
│
├─[3] UserService.java            (@Service)
│   ├─ verifyOtp(email, otp)
│   ├─ 1. findByEmail(email) → UserRepository
│   ├─ 2. Kiểm tra emailVerified == false
│   ├─ 3. Kiểm tra verificationToken == otp
│   ├─ 4. Kiểm tra otpExpiry > NOW
│   ├─ 5. user.emailVerified = true
│   ├─ 6. user.verificationToken = null
│   ├─ 7. user.otpExpiry = null
│   └─ 8. save(user) → UserRepository
│
└─[4] UserRepository.java         (JPA)
    └─ save(user) → UPDATE users SET email_verified=true WHERE email=?
```

### 3.2 Chi tiết từng file

#### [2] `AuthApiController.java` - Verify OTP Endpoint

```java
@PostMapping("/verify-otp")
public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
    String email = body.getOrDefault("email", "").trim();
    String otp = body.getOrDefault("otp", "").trim();

    // Validate input
    if (email.isEmpty() || otp.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Vui lòng nhập đầy đủ email và mã OTP."));
    }

    try {
        // Gọi service
        User user = userService.verifyOtp(email, otp);

        return ResponseEntity.ok(ApiResponse.success(
            "Xác nhận email thành công! Bạn có thể đăng nhập ngay bây giờ.",
            Map.of("email", user.getEmail(), "name", user.getName())));
    } catch (Exception e) {
        // Bắt tất cả exception: ResourceNotFoundException,
        // BusinessValidationException, ...
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

**HTTP Request:**
```
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
    "email": "nguyenvana@email.com",
    "otp": "385921"
}
```

**HTTP Response:**
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

#### [3] `UserService.java` - Verify OTP Logic

```java
@Transactional
public User verifyOtp(String email, String otp) {
    // Bước 1: Tìm user theo email
    // SELECT * FROM users WHERE email = 'nguyenvana@email.com'
    User user = userRepository.findByEmail(email);
    if (user == null) {
        throw new ResourceNotFoundException(
            "Không tìm thấy tài khoản với email này");
    }

    // Bước 2: Kiểm tra đã xác thực chưa
    // Nếu đã xác thực rồi → không cho verify lại
    if (user.isEmailVerified()) {
        throw new BusinessValidationException(
            "Tài khoản đã được xác nhận rồi.");
    }

    // Bước 3: Kiểm tra OTP có khớp không
    // So sánh chuỗi: verification_token == otp (6 số)
    if (user.getVerificationToken() == null ||
        !user.getVerificationToken().equals(otp)) {
        throw new BusinessValidationException(
            "Mã OTP không đúng. Vui lòng kiểm tra lại!");
    }

    // Bước 4: Kiểm tra OTP còn hạn không
    // otpExpiry = thời điểm hết hạn
    // NOW = thời điểm hiện tại
    // Nếu NOW > otpExpiry → đã hết hạn
    if (user.getOtpExpiry() == null ||
        user.getOtpExpiry().before(new Timestamp(System.currentTimeMillis()))) {
        throw new BusinessValidationException(
            "Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
    }

    // Bước 5: Xác thực thành công - cập nhật user
    user.setEmailVerified(true);         // true = đã xác thực
    user.setVerificationToken(null);   // Xóa OTP (không dùng nữa)
    user.setOtpExpiry(null);            // Xóa expiry (không dùng nữa)

    // Bước 6: Lưu vào DB
    // UPDATE users
    // SET email_verified = true,
    //     verification_token = NULL,
    //     otp_expiry = NULL
    // WHERE id = ?
    User updatedUser = userRepository.save(user);

    return updatedUser;
}
```

**Database state sau khi verify thành công:**
```sql
SELECT id, email, email_verified, verification_token, otp_expiry
FROM users WHERE email = 'nguyenvana@email.com';

+----+------------------------+---------+-------------------+--------+
| id | email                 | verified| verification_token| expiry |
+----+------------------------+---------+-------------------+--------+
| 10 | nguyenvana@email.com  | true    | NULL              | NULL   |
+----+------------------------+---------+-------------------+--------+
```

---

## 4. Sơ đồ Sequence

### 4.1 Đăng ký + OTP

```
Client          Controller        Service         Repository       Email
  |                |                |                |               |
  |--POST /reg----→|                |                |               |
  |                |--register--→|                |               |
  |                |               |--findByEmail→|               |
  |                |               |←─────────────| (null)        |
  |                |               |--save(user)→|               |
  |                |               |←──────────────| (user.id=10)  |
  |                |               |--sendOtpEmail→(async)       |
  |                |               |                |               |--SMTP→|
  |                |               |←──────────────|               |       |
  |                |←200 OK-------|                |               |       |
  |←───────────ApiResponse        |                |               |       |
  |                                                                 |
  |  (Email OTP arrives in inbox)                                    |
  |                                                                 |
  |--POST /verify-otp (with OTP)──→|                               |
  |                |--verifyOtp→|                |               |
  |                |               |--findByEmail→|               |
  |                |               |←─────────────| (user)        |
  |                |               |--VALIDATE OTP--|               |
  |                |               |  ├─ emailVerified? (false ✓) |
  |                |               |  ├─ token == otp? (true ✓)  |
  |                |               |  └─ expiry > NOW? (true ✓)  |
  |                |               |--save(user)→|               |
  |                |               |   (emailVerified=true)       |
  |                |               |←──────────────|               |
  |                |←200 OK-------|                |               |
  |←───────────ApiResponse (success)                             |
```

### 4.2 So sánh đăng ký vs verify OTP

| Khía cạnh | Đăng ký | Verify OTP |
|-----------|---------|-----------|
| Endpoint | POST `/auth/register` | POST `/auth/verify-otp` |
| Public/Auth | Public | Public |
| Validate | Input (name, email, password) | OTP (6 số) |
| DB Operation | INSERT | UPDATE |
| Email | Gửi OTP | Không |
| Exception | `DuplicateResourceException` | `BusinessValidationException` |

---

## 5. Chi tiết từng bước

### Bước 1: Client gửi request đăng ký

```http
POST /api/v1/auth/register HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
    "name": "Nguyen Van A",
    "email": "nguyenvana@email.com",
    "phone": "0901234567",
    "password": "12345678",
    "confirmPassword": "12345678"
}
```

### Bước 2: JwtAuthFilter kiểm tra (nhưng skip vì permitAll)

```java
// JwtAuthFilter.java - doFilterInternal()
String requestURI = request.getRequestURI();
// "/api/v1/auth/register" → starts with "/api/v1/auth/"
// → SecurityConfig: .requestMatchers("/api/v1/auth/**").permitAll()
// → Không kiểm tra JWT, cho request đi qua
```

### Bước 3: AuthApiController nhận request

```java
// AuthApiController.java
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
    // Spring tự deserialize JSON → Map
    // {"name": "Nguyen Van A", ...} → Map<String, String>
    String name = body.getOrDefault("name", "").trim();
    // ...
    return userService.registerClient(name, email, phone, password);
}
```

### Bước 4: UserService xử lý business logic

```java
// UserService.java - registerClient()
// 1. Validate email uniqueness
User existing = userRepository.findByEmail(email);
// → SELECT * FROM users WHERE email = 'nguyenvana@email.com'
// → Return: null (chưa tồn tại → tiếp tục)

// 2. Generate OTP
String otp = String.format("%06d", new Random().nextInt(999999));
// → VD: "385921"

// 3. Create entity
User user = new User();
user.setEmailVerified(false);
user.setVerificationToken(otp);  // Lưu OTP để verify

// 4. BCrypt encode password
user.setPassword(passwordEncoder.encode("12345678"));
// → "12345678" → "$2a$10$Hd2iSSXPv5GNck5IxLtQN..."

// 5. Persist
userRepository.save(user);
// → INSERT INTO users (...) VALUES (...)

// 6. Send email (async)
emailService.sendOtpEmail(email, name, otp);
// → Chạy ở thread khác, không block response
```

### Bước 5: Database insert

```sql
-- UserRepository.save() → JPA → Hibernate → JDBC → MySQL
INSERT INTO users (
    name, email, phone, password, role,
    email_verified, verification_token, otp_expiry, created_at
) VALUES (
    'Nguyen Van A',
    'nguyenvana@email.com',
    '0901234567',
    '$2a$10$Hd2iSSXPv5GNck5IxLtQN.ykL74PRyFrAun9H/DPqU28qcrkj1B4y',
    'client',
    false,
    '385921',
    '2026-05-28 06:46:00',
    '2026-05-28 06:31:00'
);
-- Result: user.id = 10
```

### Bước 6: Email gửi async

```java
// EmailService.java - @Async method
// Chạy ở thread pool riêng (Spring @Async)
// → Request trả về NGAY cho client
// → Email gửi ở background
// → Không ảnh hưởng nếu SMTP chậm hoặc fail
@Async
public void sendOtpEmail(String toEmail, String name, String otp) {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    helper.setTo(toEmail);
    helper.setSubject("Mã xác nhận OTP");
    helper.setText(buildHtml(name, otp), true);
    mailSender.send(message);  // Gửi thật sự
}
```

### Bước 7: Client nhận response

```json
// HTTP 200 OK
{
    "success": true,
    "message": "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.",
    "data": {
        "email": "nguyenvana@email.com"
    },
    "timestamp": "2026-05-28T06:31:00"
}
```

---

## 6. Tài liệu liên quan

| File | Đường dẫn | Vai trò |
|------|-----------|--------|
| `AuthApiController.java` | `controllers/api/AuthApiController.java` | Nhận request, routing |
| `UserService.java` | `services/UserService.java` | Business logic |
| `UserRepository.java` | `repositories/UserRepository.java` | CRUD database |
| `EmailService.java` | `services/EmailService.java` | Gửi email OTP |
| `ApiResponse.java` | `dto/response/ApiResponse.java` | Wrapper response |
| `User.java` | `models/entities/User.java` | Entity mapping |
| `JwtAuthFilter.java` | `filter/JwtAuthFilter.java` | Security filter |
| `SecurityConfig.java` | `config/SecurityConfig.java` | Security config |
| `DuplicateResourceException.java` | `exception/DuplicateResourceException.java` | Exception |
| `BusinessValidationException.java` | `exception/BusinessValidationException.java` | Exception |
| `GlobalExceptionHandler.java` | `exception/GlobalExceptionHandler.java` | Bắt exception |
| `DEV_GUIDE.md` | `DEV_GUIDE.md` | Tài liệu tổng hợp |

## Phụ lục: Cấu hình email

### application.properties
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Cách lấy App Password Gmail
1. Vào https://myaccount.google.com
2. Security → 2-Step Verification → Bật
3. Security → App passwords → Tạo mới
4. Copy 16 ký tự (VD: `abcd efgh ijkl mnop` → `abcdefghijklmnop`)

### Async Configuration (tự động có sẵn)
```java
// Không cần config thêm - Spring Boot tự tạo thread pool cho @Async
// Default: 8 threads cho @Async tasks
// Thay đổi nếu cần:
@Bean
public TaskExecutor asyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("EmailAsync-");
    executor.initialize();
    return executor;
}
```
