# FEATURE: User Login

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Các file tham gia](#2-các-file-tham-gia)
3. [Sơ đồ luồng kỹ thuật](#3-sơ-đồ-luồng-kỹ-thuật)
4. [Chi tiết từng file](#4-chi-tiết-từng-file)
5. [Sơ đồ Sequence](#5-sơ-đồ-sequence)
6. [Chi tiết từng bước](#6-chi-tiết-từng-bước)
7. [Sơ đồ Exception/Error](#7-sơ-đồ-exceptionerror)
8. [DEV_GUIDE.md - Phần code mẫu](#8-dev_guidemd---phần-code-mẫu)
9. [Tài liệu liên quan](#9-tài-liệu-liên-quan)

---

## 1. Tổng quan

### Mô tả
Khách hàng (client) nhập email và mật khẩu → Hệ thống xác thực thông tin đăng nhập → Nếu hợp lệ, trả về JWT token → Client dùng token này để gọi các API cần xác thực (CRUD reservation, review, ...).

### Điều kiện tiên quyết
- Tài khoản đã **đăng ký** thành công (user tồn tại trong DB)
- Tài khoản đã **xác thực email** qua OTP (emailVerified = true)
- Tài khoản **chưa bị xóa** (deletedAt = null)

### Kết quả đầu ra
- **Thành công:** JWT token dạng chuỗi dài (Bearer token)
- **Thất bại:** Thông báo lỗi cụ thể (sai password, chưa xác thực email, ...)

### Phân biệt các loại đăng nhập

| Loại user | Đường dẫn | Role trong JWT |
|-----------|-----------|---------------|
| Admin | `/api/v1/auth/login` | ROLE_ADMIN |
| Staff | `/api/v1/auth/login` | ROLE_STAFF |
| Client | `/api/v1/auth/login` | ROLE_CLIENT |

> Tất cả đều dùng cùng 1 endpoint `/auth/login`, phân biệt bằng trường `role` trong user.

---

## 2. Các file tham gia

### 2.1 Danh sách đầy đủ

```
REQUEST HTTP
    │
    ▼
AuthApiController.java          ← [1] Endpoint entry point (login)
    │ POST /auth/login
    │
    ▼
AuthenticationManager           ← [2] Spring Security xác thực
    │ (built-in)
    │
    ├──► CustomUserDetailsService ← [3] Load user từ DB
    │       │
    │       ▼
    │   UserRepository.java        ← [4] Query user
    │       │
    │       ▼
    │   BCryptPasswordEncoder      ← [5] So sánh password
    │
    ▼
JwtTokenProvider.java           ← [6] Tạo JWT token
    │
    ▼
ApiResponse.java                ← [7] Wrapper response
```

### 2.2 Mô tả từng file

| # | File | Vai trò |
|---|------|---------|
| 1 | `AuthApiController.java` | Nhận request login, gọi AuthenticationManager, trả về JWT |
| 2 | `AuthenticationManager` | Interface của Spring Security, orchestrate quá trình xác thực |
| 3 | `CustomUserDetailsService.java` | Load User entity từ DB theo email, trả về Spring Security UserDetails |
| 4 | `UserRepository.java` | Query user từ database |
| 5 | `BCryptPasswordEncoder` | So sánh password người dùng nhập với hash trong DB |
| 6 | `JwtTokenProvider.java` | Sinh JWT token chứa email + expiry |
| 7 | `ApiResponse.java` | Wrapper response chuẩn hóa |

---

## 3. Sơ đồ luồng kỹ thuật

### 3.1 Luồng hoàn chỉnh (khi thành công)

```
HTTP POST /api/v1/auth/login
│
├─[1] JwtAuthFilter.java              (Security filter - kiểm tra JWT token)
│   ├─ skip: /api/v1/auth/** → permitAll
│   └─ cho request đi qua (chưa có token, nhưng endpoint này permitAll)
│
├─[2] AuthApiController.java           (@RestController - nhận request)
│   ├─ @PostMapping("/login")
│   ├─ @RequestBody Map<String, String> body
│   ├─ Extract: email, password
│   ├─ Validate input
│   ├─ Gọi: authenticationManager.authenticate(usernamePasswordToken)
│   └─ Gọi: jwtTokenProvider.generateTokenFromEmail(email)
│
├─[3] AuthenticationManager           (Spring Security - xác thực)
│   ├─ Nhận UsernamePasswordAuthenticationToken
│   ├─ Gọi: CustomUserDetailsService.loadUserByUsername(email)
│   │   → UserRepository.findByEmail(email)
│   │   → Tạo Spring UserDetails object
│   └─ Gọi: BCryptPasswordEncoder.matches(password, hashedPassword)
│       → So sánh password nhập vs hash trong DB
│
├─[4] CustomUserDetailsService.java    (@Service - Spring Security integration)
│   ├─ @Override loadUserByUsername(email)
│   ├─ userRepository.findByEmail(email)
│   ├─ Tạo org.springframework.security.core.userdetails.User
│   │   - username = email
│   │   - password = hashed password từ DB
│   │   - authorities = ["ROLE_CLIENT"] (hoặc ADMIN/STAFF)
│   └─ Return: UserDetails
│
├─[5] UserRepository.java             (JPA Repository)
│   ├─ findByEmail(String email)
│   └─ SELECT * FROM users WHERE email = ? AND deleted_at IS NULL
│
├─[6] JwtTokenProvider.java           (@Component - tạo token)
│   ├─ @Value("${app.jwtSecret}")
│   ├─ @Value("${app.jwtExpirationMs}")
│   ├─ generateTokenFromEmail(String email)
│   ├─ Tạo JWT với: subject=email, iat=NOW, exp=NOW+24h
│   └─ Sign: HS256 với secret key
│
└─[7] ApiResponse.java                (Wrapper response)
    └─ return: {"success":true, "message":"...", "data":{token, user}}
```

### 3.2 Điểm khác biệt với Đăng ký

| Khía cạnh | Đăng ký (Register) | Đăng nhập (Login) |
|-----------|-------------------|-------------------|
| **HTTP Method** | POST `/auth/register` | POST `/auth/login` |
| **Parameters** | name, email, phone, password | email, password |
| **Database** | INSERT (tạo user mới) | SELECT (tìm user có sẵn) |
| **Password** | Encode (BCrypt) → lưu | Decode (BCrypt.matches) → so sánh |
| **Email** | Gửi OTP | Không gửi |
| **@Async** | Có (gửi email) | Không |
| **Service** | UserService.registerClient() | AuthenticationManager.authenticate() |
| **Token** | Không tạo | Tạo JWT token |
| **Kiểm tra** | Email chưa tồn tại | Email tồn tại, password đúng |

---

## 4. Chi tiết từng file

### 4.1 [1] `AuthApiController.java` - Controller

```java
// File: src/main/java/com/example/qlnh/controllers/api/AuthApiController.java

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final IUserService userService;

    /**
     * POST /api/v1/auth/login
     * Đăng nhập bằng email và password.
     * Trả về JWT token nếu thành công.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        // ─────────────────────────────────────────────────────────
        // BƯỚC 1: Extract params từ request body
        // ─────────────────────────────────────────────────────────
        String email = body.getOrDefault("email", "").trim();
        String password = body.getOrDefault("password", "").trim();

        // ─────────────────────────────────────────────────────────
        // BƯỚC 2: Validate input cơ bản
        // ─────────────────────────────────────────────────────────
        if (email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Vui lòng nhập email và mật khẩu."));
        }

        // ─────────────────────────────────────────────────────────
        // BƯỚC 3: Kiểm tra user có tồn tại và đã xác thực email
        // ─────────────────────────────────────────────────────────
        User user = userService.getUserByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
        }

        if (!user.isEmailVerified()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                    "Tài khoản chưa được xác nhận. Vui lòng kiểm tra email và xác nhận trước khi đăng nhập."));
        }

        // ─────────────────────────────────────────────────────────
        // BƯỚC 4: Xác thực credentials qua Spring Security
        // ─────────────────────────────────────────────────────────
        // AuthenticationManager sẽ:
        // 1. Gọi CustomUserDetailsService.loadUserByUsername(email)
        // 2. So sánh password nhập với hash trong DB bằng BCrypt
        // 3. Nếu khớp → trả về Authentication object
        // 4. Nếu sai → ném AuthenticationException
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            // authentication.isAuthenticated() = true nếu xác thực thành công
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Đăng nhập thất bại. Vui lòng thử lại."));
        }

        // ─────────────────────────────────────────────────────────
        // BƯỚC 5: Generate JWT token
        // ─────────────────────────────────────────────────────────
        String token = jwtTokenProvider.generateTokenFromEmail(email);

        // ─────────────────────────────────────────────────────────
        // BƯỚC 6: Build response data
        // ─────────────────────────────────────────────────────────
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);                          // JWT token
        data.put("tokenType", "Bearer");                   // Luôn là "Bearer"
        data.put("email", user.getEmail());
        data.put("name", user.getName());
        data.put("role", user.getRole());

        return ResponseEntity.ok(ApiResponse.success(
            "Đăng nhập thành công!", data));
    }
}
```

**HTTP Request Example:**
```
POST /api/v1/auth/login
Content-Type: application/json

{
    "email": "nguyenvana@email.com",
    "password": "12345678"
}
```

**HTTP Response Example (thành công):**
```json
{
    "success": true,
    "message": "Đăng nhập thành công!",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzQ4...",
        "tokenType": "Bearer",
        "email": "nguyenvana@email.com",
        "name": "Nguyen Van A",
        "role": "client"
    },
    "timestamp": "2026-05-29T22:53:00"
}
```

**HTTP Response Example (thất bại - chưa xác thực):**
```json
{
    "success": false,
    "message": "Tài khoản chưa được xác nhận. Vui lòng kiểm tra email và xác nhận trước khi đăng nhập.",
    "timestamp": "2026-05-29T22:53:00"
}
```

**HTTP Response Example (thất bại - sai password):**
```json
{
    "success": false,
    "message": "Email hoặc mật khẩu không đúng.",
    "timestamp": "2026-05-29T22:53:00"
}
```

### 4.2 [2] `AuthenticationManager` - Spring Security Authentication

```java
// AuthenticationManager là interface của Spring Security
// Trong SecurityConfig.java, nó được tạo từ AuthenticationConfiguration:

// SecurityConfig.java - phần cấu hình:
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
}

// Cách hoạt động:
// 1. authenticationManager.authenticate(token) được gọi
// 2. Spring tìm Provider phù hợp (DaoAuthenticationProvider)
// 3. DaoAuthenticationProvider:
//    a. Gọi CustomUserDetailsService.loadUserByUsername(email)
//       → Lấy UserDetails (username, password hash, authorities)
//    b. Gọi PasswordEncoder.matches(rawPassword, hashedPassword)
//       → BCrypt so sánh "12345678" vs "$2a$10$..."
//    c. Nếu khớp → tạo Authentication object
//    d. Nếu không khớp → ném BadCredentialsException
```

### 4.3 [3] `CustomUserDetailsService.java` - Spring Security Integration

```java
// File: src/main/java/com/example/qlnh/services/CustomUserDetailsService.java

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user từ database theo email.
     * Được gọi bởi AuthenticationManager khi xác thực đăng nhập.
     *
     * @param email - email của user (dùng làm username trong Spring Security)
     * @return org.springframework.security.core.userdetails.User
     * @throws UsernameNotFoundException - khi không tìm thấy user
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // ─────────────────────────────────────────────────────────
        // Tìm user trong database theo email
        // ─────────────────────────────────────────────────────────
        User user = userRepository.findByEmail(email);

        // ─────────────────────────────────────────────────────────
        // Nếu không tìm thấy → ném exception
        // Spring Security sẽ bắt và xử lý
        // ─────────────────────────────────────────────────────────
        if (user == null) {
            throw new UsernameNotFoundException(
                "Không tìm thấy tài khoản với email: " + email);
        }

        // ─────────────────────────────────────────────────────────
        // Chuyển đổi User entity → Spring Security UserDetails
        // ─────────────────────────────────────────────────────────
        // Spring Security UserDetails cần:
        // - username: identifier (ở đây dùng email)
        // - password: BCrypt hash từ DB
        // - authorities: danh sách quyền (roles)
        //
        // Cách Spring xác định authority:
        // - role = "admin" → authorities = ["ROLE_ADMIN"]
        // - role = "staff" → authorities = ["ROLE_STAFF"]
        // - role = "client" → authorities = ["ROLE_CLIENT"]
        String role = user.getRole();
        String authority = switch (role) {
            case "admin"  -> "ROLE_ADMIN";
            case "staff"  -> "ROLE_STAFF";
            case "client" -> "ROLE_CLIENT";
            default       -> "ROLE_CLIENT";
        };

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),                                    // username = email
            user.getPassword(),                                 // BCrypt hashed password
            Collections.singletonList(() -> authority)           // authorities = [ROLE_CLIENT]
        );
    }
}
```

### 4.4 [4] `UserRepository.java` - Data Access

```java
// File: src/main/java/com/example/qlnh/repositories/UserRepository.java

public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository đã có sẵn:
    // save(User) → INSERT / UPDATE
    // findById(Long) → SELECT ... WHERE id = ?
    // findAll() → SELECT * FROM users
    // deleteById(Long) → DELETE ...

    // Custom query - định nghĩa bởi developer:
    User findByEmail(String email);
    // → Spring tự generate:
    // SELECT * FROM users WHERE email = ? AND deleted_at IS NULL

    User findByEmailOrPhone(String email, String phone);
    // → SELECT * FROM users WHERE email = ? OR phone = ?

    List<User> findByRole(String role);
    // → SELECT * FROM users WHERE role = ?

    Long checkEmailExist(String email, Long id);
    // → SELECT COUNT(*) FROM users WHERE email = ? AND id != ?
}
```

### 4.5 [5] `BCryptPasswordEncoder` - Password Comparison

```java
// BCryptPasswordEncoder được định nghĩa trong SecurityConfig.java:

@Bean
public static BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// Cách hoạt động:
// 1. Khi đăng ký (registerClient):
//    user.setPassword(passwordEncoder.encode("12345678"));
//    → BCrypt hash: "12345678" → "$2a$10$Hd2iSSXPv5GNck5IxLtQN..."
//    → Lưu vào DB
//
// 2. Khi đăng nhập (authenticationManager.authenticate):
//    BCryptPasswordEncoder.matches(rawPassword, hashedPassword)
//    → So sánh "12345678" (user nhập) với "$2a$10$..." (DB)
//    → BCrypt tự decode và so sánh
//    → Trả về true nếu khớp, false nếu sai
//
// Đặc điểm của BCrypt:
// - Salt được tự động thêm vào hash (mỗi lần encode cho ra kết quả khác nhau)
// - So sánh bằng .matches() (không phải equals)
// - Slow by design (chống brute force)
```

### 4.6 [6] `JwtTokenProvider.java` - JWT Token Generation

```java
// File: src/main/java/com/example/qlnh/helpers/JwtTokenProvider.java

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${app.jwtSecret:MySuperSecretKeyForJWT123!@#4567890abcdefghijklmnopqrstuvwxyz}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;  // 86400000ms = 24 hours

    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Tạo JWT token từ email.
     * Token chứa:
     *   - subject (sub): email của user
     *   - issuedAt (iat): thời điểm tạo
     *   - expiration (exp): thời điểm hết hạn
     */
    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(email)          // ← Email làm identifier
                .setIssuedAt(now)            // ← Thời điểm tạo
                .setExpiration(expiryDate)   // ← Hết hạn sau 24h
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract email từ JWT token.
     * Được gọi bởi JwtAuthFilter khi có request cần xác thực.
     */
    public String getUserEmailFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();  // ← Lấy email từ subject
    }

    /**
     * Validate JWT token.
     * Kiểm tra:
     *   - Token không bị null/empty
     *   - Token format đúng
     *   - Token chưa hết hạn
     *   - Signature hợp lệ
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**Cấu trúc JWT Token:**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzQ4
MDAwMDAwLCJleHAiOjE3NDgwODM2MDB9.abc123xyz

├── Header (eyJhbGciOiJIUzI1NiJ9)
│   {"alg":"HS256","typ":"JWT"}
├── Payload (eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzQ4...)
│   {
│     "sub": "test@example.com",      ← Email của user
│     "iat": 1748000000,               ← Issued at (timestamp)
│     "exp": 1748083600                ← Expiration (timestamp)
│   }
└── Signature (.abc123xyz)
    HMAC-SHA256(header.payload, secret_key)
```

### 4.7 [7] `ApiResponse.java` - Response Wrapper

```java
// File: src/main/java/com/example/qlnh/dto/response/ApiResponse.java

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

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

## 5. Sơ đồ Sequence

### 5.1 Đăng nhập thành công

```
Client          Controller        AuthManager      UserDetailsSvc    Repository        JwtProvider
  |                |                |                  |                |                |
  |--POST /login──→|                |                  |                |                |
  |  (email,pwd)   |                |                  |                |                |
  |                |--authenticate()→                  |                |                |
  |                |               |--loadUserByUsername(email)→        |                |
  |                |               |                  |--findByEmail→ |                |
  |                |               |                  |               |--SELECT──→   |
  |                |               |                  |               ←─────────────|
  |                |               |                  |←───UserDetails──|               |
  |                |               |                  |                |                |
  |                |               |--BCrypt.matches(raw, hashed)───→|                |
  |                |               |                  |               ←──true───|       |
  |                |               |←───Authentication (authenticated)──|               |
  |                |←─────────────|                  |                |                |
  |                |                |                  |                |                |
  |                |--generateTokenFromEmail(email)→|                |                |
  |                |               |                  |                |←───JWT───────|
  |                |←─────────────|                  |                |                |
  |←──200 OK──────|                |                  |                |                |
  |  {token,user} |                |                  |                |                |
```

### 5.2 Đăng nhập thất bại - User không tồn tại

```
Client          Controller        UserService       Repository
  |                |                |                |
  |--POST /login──→|                |                |
  |  (email,pwd)   |                |                |
  |                |--getUserByEmail(email)→        |
  |                |               |--findByEmail→ |                |
  |                |               |               ←───null────|
  |                |               |←──────────────|                |
  |                |←──────────────|                |                |
  |←──400 BadRequest──|                |                |
  {"success":false,"message":"Email hoặc mật khẩu không đúng."}
```

### 5.3 Đăng nhập thất bại - Chưa xác thực email

```
Client          Controller        UserService
  |                |                |
  |--POST /login──→|                |
  |  (email,pwd)   |                |
  |                |--getUserByEmail(email)→|
  |                |               |--findByEmail→|
  |                |               |←───user─────|
  |                |               |              |
  |                |               |--emailVerified?→|
  |                |               |←───false────|
  |                |←──────────────|              |
  |←──400 BadRequest──|              |
  {"success":false,"message":"Tài khoản chưa được xác nhận..."}
```

### 5.4 Đăng nhập thất bại - Sai password

```
Client          Controller        AuthManager       UserDetailsSvc
  |                |                |                |
  |--POST /login──→|                |                |
  |  (email,pwd)   |                |                |
  |                |--authenticate()→               |
  |                |               |--loadUserByUsername(email)→|
  |                |               |←───UserDetails──|
  |                |               |                  |
  |                |               |--BCrypt.matches(wrong, hashed)→|
  |                |               |←───false────|
  |                |               |                  |
  |                |               |--THROW BadCredentialsException
  |                |←───────────────|
  |←──400 BadRequest──|
  {"success":false,"message":"Email hoặc mật khẩu không đúng."}
```

---

## 6. Chi tiết từng bước

### Bước 1: Client gửi request đăng nhập

```http
POST /api/v1/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
    "email": "nguyenvana@email.com",
    "password": "12345678"
}
```

### Bước 2: JwtAuthFilter kiểm tra (skip vì permitAll)

```java
// JwtAuthFilter.java - doFilterInternal()
String requestURI = request.getRequestURI();
// "/api/v1/auth/login" → starts with "/api/v1/auth/"
// → SecurityConfig: .requestMatchers("/api/v1/auth/**").permitAll()
// → Không kiểm tra JWT, cho request đi qua
```

### Bước 3: AuthApiController nhận request

```java
// AuthApiController.java - login()
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    String email = body.getOrDefault("email", "").trim();
    String password = body.getOrDefault("password", "").trim();

    // Validate
    if (email.isEmpty() || password.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Vui lòng nhập email và mật khẩu."));
    }

    // Kiểm tra user tồn tại
    User user = userService.getUserByEmail(email);
    if (user == null) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
    }

    // Kiểm tra email đã xác thực chưa
    if (!user.isEmailVerified()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "Tài khoản chưa được xác nhận. Vui lòng kiểm tra email và xác nhận trước khi đăng nhập."));
    }

    // Xác thực credentials
    try {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
    } catch (BadCredentialsException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
    }

    // Generate JWT token
    String token = jwtTokenProvider.generateTokenFromEmail(email);

    // Build response
    Map<String, Object> data = new HashMap<>();
    data.put("token", token);
    data.put("tokenType", "Bearer");
    data.put("email", user.getEmail());
    data.put("name", user.getName());
    data.put("role", user.getRole());

    return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", data));
}
```

### Bước 4: Spring Security xác thực

```java
// AuthenticationManager.authenticate() thực hiện:
// 1. DaoAuthenticationProvider nhận request
// 2. Gọi CustomUserDetailsService.loadUserByUsername(email)
UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
// → userDetails.username = "nguyenvana@email.com"
// → userDetails.password = "$2a$10$Hd2iSSXPv5GNck5IxLtQN..."
// → userDetails.authorities = [ROLE_CLIENT]

// 3. Gọi BCryptPasswordEncoder.matches(password, userDetails.password)
bcryptPasswordEncoder.matches("12345678", "$2a$10$Hd2iSSXPv5GNck5IxLtQN...");
// → true (nếu password đúng)

// 4. Tạo Authentication object
Authentication auth = new UsernamePasswordAuthenticationToken(
    userDetails, null, userDetails.getAuthorities());
// → auth.isAuthenticated() = true
```

### Bước 5: CustomUserDetailsService load user

```java
// CustomUserDetailsService.java - loadUserByUsername()
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // SELECT * FROM users WHERE email = 'nguyenvana@email.com'
    User user = userRepository.findByEmail(email);
    if (user == null) {
        throw new UsernameNotFoundException("Không tìm thấy tài khoản...");
    }

    String authority = switch (user.getRole()) {
        case "admin"  -> "ROLE_ADMIN";
        case "staff"  -> "ROLE_STAFF";
        default       -> "ROLE_CLIENT";
    };

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),  // BCrypt hash từ DB
        Collections.singletonList(() -> authority)
    );
}
```

### Bước 6: JwtTokenProvider tạo token

```java
// JwtTokenProvider.java - generateTokenFromEmail()
public String generateTokenFromEmail(String email) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 86400000); // +24h

    return Jwts.builder()
            .setSubject(email)        // "nguyenvana@email.com"
            .setIssuedAt(now)         // Timestamp tạo
            .setExpiration(expiryDate) // Timestamp hết hạn
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
}
```

### Bước 7: Client nhận response

```json
// HTTP 200 OK
{
    "success": true,
    "message": "Đăng nhập thành công!",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "tokenType": "Bearer",
        "email": "nguyenvana@email.com",
        "name": "Nguyen Van A",
        "role": "client"
    },
    "timestamp": "2026-05-29T22:53:00"
}
```

---

## 7. Sơ đồ Exception/Error

### 7.1 Tất cả trường hợp lỗi

| # | Trường hợp | Kiểm tra ở đâu | HTTP Status | Message |
|---|-----------|---------------|-------------|---------|
| 1 | Input rỗng | Controller | 400 | Vui lòng nhập email và mật khẩu |
| 2 | Email không tồn tại | Controller (kiểm tra trước khi authenticate) | 400 | Email hoặc mật khẩu không đúng |
| 3 | Chưa xác thực email | Controller (kiểm tra trước khi authenticate) | 400 | Tài khoản chưa được xác nhận |
| 4 | Sai password | AuthenticationManager (BadCredentialsException) | 400 | Email hoặc mật khẩu không đúng |
| 5 | User bị xóa (soft delete) | UserRepository (findByEmail tự loại trừ) | 400 | Email hoặc mật khẩu không đúng |
| 6 | Lỗi server | Controller (catch all) | 500 | Đăng nhập thất bại |

### 7.2 Chi tiết exception mapping

```java
// AuthApiController.java - login() exception handling

// Trường hợp 1 & 2: Input validation
if (email.isEmpty() || password.isEmpty()) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Vui lòng nhập email và mật khẩu."));
}

// Trường hợp 3: Kiểm tra emailVerified
User user = userService.getUserByEmail(email);
if (user == null) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
}
if (!user.isEmailVerified()) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(
            "Tài khoản chưa được xác nhận. Vui lòng kiểm tra email và xác nhận trước khi đăng nhập."));
}

// Trường hợp 4: BadCredentialsException
try {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, password));
} catch (BadCredentialsException e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
}

// Trường hợp 6: Catch all
catch (Exception e) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Đăng nhập thất bại. Vui lòng thử lại."));
}
```

---

## 8. DEV_GUIDE.md - Phần code mẫu

> **XEM CHI TIẾT LUỒNG KỸ THUẬT:** [docs/features/user-login-flow.md](docs/features/user-login-flow.md) — Sơ đồ sequence, chi tiết 7 file (Controller → AuthenticationManager → UserDetailsService → Repository → BCrypt → JwtTokenProvider → ApiResponse), HTTP request/response mẫu, và các trường hợp lỗi.

### Code mẫu cho `AuthApiController.java` - Method login()

**VỊ TRÍ:** Trong class `AuthApiController`, tìm method `login`.

**ĐẦU VÀO:**
| Tham số | Kiểu | Ràng buộc | Mô tả |
|---------|------|-----------|--------|
| email | String | NOT NULL, NOT BLANK, EMAIL format | Email tài khoản |
| password | String | NOT NULL, NOT BLANK | Mật khẩu |

**ĐẦU RA:** `ResponseEntity<?>` với data chứa JWT token

**EXCEPTION:**
- `BadCredentialsException` - Sai email hoặc password

**LOGIC CẦN THỰC HIỆN:**
```
1. Extract email và password từ request body

2. Validate input:
   → Nếu rỗng → ném error "Vui lòng nhập email và mật khẩu"

3. Kiểm tra user tồn tại (userService.getUserByEmail):
   → Nếu null → ném error "Email hoặc mật khẩu không đúng"

4. Kiểm tra email đã xác thực chưa:
   → Nếu emailVerified = false → ném error "Tài khoản chưa được xác nhận..."

5. Xác thực credentials (authenticationManager.authenticate):
   → Tạo UsernamePasswordAuthenticationToken(email, password)
   → Gọi authenticationManager.authenticate(token)
   → Nếu BadCredentialsException → ném error "Email hoặc mật khẩu không đúng"
   → AuthenticationManager sẽ tự:
      a. Gọi CustomUserDetailsService.loadUserByUsername(email)
      b. Gọi BCryptPasswordEncoder.matches(password, hashedPassword)
      c. Nếu khớp → trả về Authentication object

6. Generate JWT token:
   → jwtTokenProvider.generateTokenFromEmail(email)

7. Build response data:
   → token: JWT string
   → tokenType: "Bearer"
   → email: email của user
   → name: tên của user
   → role: role của user

8. Trả về ResponseEntity.ok với ApiResponse.success
```

**VÍ DỤ CODE ĐẦY ĐỦ:**

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
    String email = body.getOrDefault("email", "").trim();
    String password = body.getOrDefault("password", "").trim();

    // Validate input
    if (email.isEmpty() || password.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Vui lòng nhập email và mật khẩu."));
    }

    // Kiểm tra user tồn tại
    User user = userService.getUserByEmail(email);
    if (user == null) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
    }

    // Kiểm tra email đã xác thực chưa
    if (!user.isEmailVerified()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(
                "Tài khoản chưa được xác nhận. Vui lòng kiểm tra email và xác nhận trước khi đăng nhập."));
    }

    // Xác thực credentials qua Spring Security
    try {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password)
        );
    } catch (BadCredentialsException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Email hoặc mật khẩu không đúng."));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Đăng nhập thất bại. Vui lòng thử lại."));
    }

    // Generate JWT token
    String token = jwtTokenProvider.generateTokenFromEmail(email);

    // Build response data
    Map<String, Object> data = new HashMap<>();
    data.put("token", token);
    data.put("tokenType", "Bearer");
    data.put("email", user.getEmail());
    data.put("name", user.getName());
    data.put("role", user.getRole());

    return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", data));
}
```

### Code mẫu cho `CustomUserDetailsService.java` (nếu cần implement)

> **LƯU Ý:** CustomUserDetailsService đã được tạo sẵn trong project. Kiểm tra file `src/main/java/com/example/qlnh/services/CustomUserDetailsService.java` trước khi code lại.

---

## 9. Tài liệu liên quan

| File | Đường dẫn | Vai trò |
|------|-----------|---------|
| `AuthApiController.java` | `controllers/api/AuthApiController.java` | Nhận request login, routing |
| `CustomUserDetailsService.java` | `services/CustomUserDetailsService.java` | Load user cho Spring Security |
| `JwtTokenProvider.java` | `helpers/JwtTokenProvider.java` | Generate JWT token |
| `UserRepository.java` | `repositories/UserRepository.java` | Query user từ DB |
| `ApiResponse.java` | `dto/response/ApiResponse.java` | Wrapper response |
| `User.java` | `models/entities/User.java` | Entity mapping |
| `JwtAuthFilter.java` | `filter/JwtAuthFilter.java` | Security filter |
| `SecurityConfig.java` | `config/SecurityConfig.java` | Security config (BCrypt bean, AuthManager) |
| `IUserService.java` | `services/IUserService.java` | Service interface |
| `DEV_GUIDE.md` | `DEV_GUIDE.md` | Tài liệu tổng hợp |
| `user-registration-otp.md` | `docs/features/user-registration-otp.md` | Tài liệu đăng ký + OTP |

## Phụ lục: JWT Token được sử dụng như thế nào?

### Sau khi login thành công

Client lưu JWT token và gửi kèm trong header cho mọi request cần xác thực:

```http
GET /api/v1/client/reservations HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzQ4...
```

### JwtAuthFilter xử lý token

```java
// JwtAuthFilter.java - doFilterInternal()
String jwt = getJwtFromRequest(request);  // Lấy "eyJhbGci..." từ header
if (jwt != null && tokenProvider.validateToken(jwt)) {
    String email = tokenProvider.getUserEmailFromJWT(jwt);  // Lấy email từ token
    User user = userService.getUserByEmail(email);          // Load user từ DB
    if (user != null) {
        String role = user.getRole();
        String grantedRole = switch (role) {
            case "admin"  -> "ROLE_ADMIN";
            case "staff"  -> "ROLE_STAFF";
            default       -> "ROLE_CLIENT";
        };
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null,
                Collections.singletonList(new SimpleGrantedAuthority(grantedRole)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
```

### Spring Security kiểm tra quyền

```
Request: GET /api/v1/admin/users
         ↓
JwtAuthFilter: Extract token → Get email → Set SecurityContext
         ↓
SecurityConfig: .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
         ↓
SecurityContextHolder: Check if user has ROLE_ADMIN
         ↓
├─ Có ROLE_ADMIN → Cho phép đi qua
└─ Không có ROLE_ADMIN → 403 Forbidden
```

## Phụ lục: Cấu hình JWT trong application.properties

```properties
# JWT Configuration
# Secret key dùng để sign JWT (phải đủ dài cho HS256)
app.jwtSecret=MySuperSecretKeyForJWT123!@#4567890abcdefghijklmnopqrstuvwxyz

# Thời gian hết hạn của token (miliseconds)
# 86400000ms = 24 hours
app.jwtExpirationMs=86400000
```

## Phụ lục: Thời gian hết hạn token

| Giá trị | Milliseconds | Giờ | Ngày |
|---------|-------------|------|------|
| 3600000 | 3,600,000 | 1 giờ | |
| 86400000 | 86,400,000 | 24 giờ | 1 ngày |
| 604800000 | 604,800,000 | 168 giờ | 7 ngày |

> **Mặc định:** 24 giờ (86400000ms)
> **Khuyến nghị:** Client nên refresh token trước khi hết hạn
