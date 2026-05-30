# TÀI LIỆU CRUD CHI TIẾT - TẤT CẢ CHỨC NĂNG

## Mục lục
1. [User CRUD](#1-user-crud)
2. [Table CRUD](#2-table-crud)
3. [Food CRUD](#3-food-crud)
4. [Combo CRUD](#4-combo-crud)
5. [Reservation CRUD](#5-reservation-crud)
6. [Client APIs](#6-client-apis)
7. [Dashboard - Thống kê](#7-dashboard---thống-kê)
8. [Tổng hợp Repository Methods](#8-tổng-hợp-repository-methods)

---

## 1. USER CRUD

### Mục lục User
- [1.1 GET /admin/users](#11-get-adminusers---danh-sách-users)
- [1.2 POST /admin/users](#12-post-adminusers---tạo-user)
- [1.3 PUT /admin/users/{id}](#13-put-adminusersid---cập-nhật-user)
- [1.4 DELETE /admin/users/{id}](#14-delete-adminusersid---xóa-user)

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
UserApiController.java          ← [1] Endpoint entry point
    │ GET/POST/PUT/DELETE /admin/users
    │
    ▼
IUserService                   ← [2] Interface
    │
    ▼
UserService.java               ← [3] Business logic
    │
    ├──► UserRepository.java      ← [4] CRUD database
    ├──► BCryptPasswordEncoder     ← [5] Encode password
    └──► CurrentUserResolver.java  ← [6] Lấy user hiện tại
```

### 1.1 GET /admin/users — Danh sách users

**Controller:** `UserApiController.listUsers()`
**Service:** `IUserService` — nhiều method

**HTTP Request:**
```
GET /api/v1/admin/users?page=1&itemsPerPage=10&role=CLIENT&keyword=nguyen
Authorization: Bearer <token>
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang hiện tại |
| itemsPerPage | int | 10 | Số item/trang |
| role | String | null | Filter theo role: ADMIN, STAFF, CLIENT |
| keyword | String | null | Tìm kiếm theo tên/email/phone |

**Logic cần thực hiện:**

```
1. Tính pageIndex = page - 1 (vì Spring Data Pageable 0-indexed)

2. Xác định method gọi dựa trên filter:
   
   A. Có keyword + có role:
      → userService.findByKeywordWithRole(keyword, role, pageIndex, itemsPerPage)
      → total = userService.getTotalUsersByKeywordWithRole(keyword, role)
   
   B. Có keyword, không có role:
      → userService.findByKeyword(keyword, pageIndex, itemsPerPage)
      → total = userService.getTotalUsersByKeyword(keyword)
   
   C. Có role, không có keyword:
      → userService.getUsersByRole(pageIndex, itemsPerPage, role)
      → total = userService.getTotalUsersByRole(role)
   
   D. Không có gì:
      → userService.getUsers(pageIndex, itemsPerPage)
      → total = userService.getTotalUsers()

3. Chuyển đổi Page<User> → Page<UserResponse>
   → userResponsePage.map(UserResponse::fromEntity)

4. Trả về ResponseEntity.ok(ApiResponse.success(data, pageResponse))
```

**Đầu ra:**
```json
{
    "success": true,
    "data": {
        "content": [
            {
                "id": 1,
                "name": "Admin User",
                "email": "admin@gmail.com",
                "phone": "0901234567",
                "role": "ADMIN",
                "createdAt": "2026-05-30T12:00:00"
            }
        ],
        "totalElements": 50,
        "totalPages": 5,
        "size": 10,
        "number": 1,
        "first": true,
        "last": false
    }
}
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
        @RequestParam(value = "role", required = false) String role,
        @RequestParam(value = "keyword", required = false) String keyword) {

    int pageIndex = page - 1; // Pageable 0-indexed
    Page<UserResponse> userPage;
    long totalElements;

    if (keyword != null && !keyword.isBlank() && role != null && !role.isBlank()) {
        userPage = userService.findByKeywordWithRole(keyword, role, pageIndex, itemsPerPage)
                .map(UserResponse::fromEntity);
        totalElements = userService.getTotalUsersByKeywordWithRole(keyword, role);
    } else if (keyword != null && !keyword.isBlank()) {
        userPage = userService.findByKeyword(keyword, pageIndex, itemsPerPage)
                .map(UserResponse::fromEntity);
        totalElements = userService.getTotalUsersByKeyword(keyword);
    } else if (role != null && !role.isBlank()) {
        userPage = userService.getUsersByRole(pageIndex, itemsPerPage, role)
                .map(UserResponse::fromEntity);
        totalElements = userService.getTotalUsersByRole(role);
    } else {
        userPage = userService.getUsers(pageIndex, itemsPerPage)
                .map(UserResponse::fromEntity);
        totalElements = userService.getTotalUsers();
    }

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách người dùng thành công",
        userPage
    ));
}
```

---

### 1.2 POST /admin/users — Tạo user

**Controller:** `UserApiController.createUser()`
**Service:** `IUserService.createUser(User user)` + `checkEmailExist` + `checkPhoneExist`

**HTTP Request:**
```
POST /api/v1/admin/users
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Nguyen Van B",
    "email": "nguyenvanb@email.com",
    "phone": "0901234567",
    "role": "STAFF",
    "password": "12345678",
    "confirmPassword": "12345678"
}
```

**Input (CreateUserRequest):**
| Field | Ràng buộc | Mô tả |
|-------|-----------|--------|
| name | @NotBlank | Họ tên |
| email | @NotBlank, @Email | Email (duy nhất) |
| phone | String | Số điện thoại (optional) |
| role | @NotBlank | ADMIN / STAFF / CLIENT |
| password | @NotBlank, >= 6 chars | Mật khẩu |
| confirmPassword | String | Xác nhận mật khẩu |

**Logic cần thực hiện:**

```
1. Validate password == confirmPassword
   → Nếu không khớp → BusinessValidationException("Mật khẩu xác nhận không khớp")

2. Kiểm tra email đã tồn tại chưa (checkEmailExist)
   → Nếu tồn tại → DuplicateResourceException("Email đã được sử dụng")

3. Kiểm tra phone đã tồn tại chưa (checkPhoneExist) — nếu phone != null
   → Nếu tồn tại → DuplicateResourceException("Số điện thoại đã được sử dụng")

4. Tạo User entity:
   - name = request.name
   - email = request.email
   - phone = request.phone
   - role = request.role (ADMIN/STAFF/CLIENT)
   - password = BCrypt.encode(request.password)
   - emailVerified = true (admin tạo → tài khoản đã xác thực sẵn)
   - createdAt = NOW

5. Lưu vào DB: userService.createUser(user)

6. Trả về: 201 Created
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody CreateUserRequest req) {
    // 1. Validate password match
    if (!userService.comparePassword(req.getPassword(), req.getConfirmPassword())) {
        throw new BusinessValidationException("Mật khẩu xác nhận không khớp");
    }

    // 2. Check email unique
    User checkUser = new User();
    checkUser.setEmail(req.getEmail());
    if (userService.checkEmailExist(checkUser)) {
        throw new DuplicateResourceException("Email đã được sử dụng: " + req.getEmail());
    }

    // 3. Check phone unique (if provided)
    if (req.getPhone() != null && !req.getPhone().isBlank()) {
        User phoneCheck = new User();
        phoneCheck.setPhone(req.getPhone());
        if (userService.checkPhoneExist(phoneCheck)) {
            throw new DuplicateResourceException("Số điện thoại đã được sử dụng: " + req.getPhone());
        }
    }

    // 4. Build entity
    User user = new User();
    user.setName(req.getName());
    user.setEmail(req.getEmail());
    user.setPhone(req.getPhone());
    user.setRole(req.getRole().toUpperCase());
    user.setPassword(passwordEncoder.encode(req.getPassword()));
    user.setEmailVerified(true); // Admin tạo → verified sẵn

    // 5. Save
    userService.createUser(user);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo người dùng thành công", null));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "message": "Tạo người dùng thành công",
    "timestamp": "2026-05-30T12:00:00"
}
```

**Exceptions:**
| Exception | HTTP | Điều kiện |
|-----------|------|-----------|
| `BusinessValidationException` | 400 | Password không khớp confirmPassword |
| `DuplicateResourceException` | 409 | Email đã tồn tại |
| `DuplicateResourceException` | 409 | Phone đã tồn tại |

---

### 1.3 PUT /admin/users/{id} — Cập nhật user

**Controller:** `UserApiController.updateUser()`
**Service:** `IUserService.updateUser(User user)` + `checkEmailExist` + `checkPhoneExist`

**HTTP Request:**
```
PUT /api/v1/admin/users/5
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Nguyen Van B Updated",
    "email": "nguyenvanb@email.com",
    "phone": "0912345678",
    "role": "STAFF",
    "password": "newpassword",
    "confirmPassword": "newpassword"
}
```

**Input (UpdateUserRequest):**
Giống CreateUserRequest nhưng password và confirmPassword là optional.

**Logic cần thực hiện:**

```
1. Tìm user cũ theo id
   → Nếu không tìm thấy → ResourceNotFoundException("Không tìm thấy người dùng")

2. Validate password match (nếu có password)
   → Nếu có password mới → so sánh với confirmPassword

3. Kiểm tra email đã bị trùng bởi user khác chưa
   → userService.checkEmailExist(user) — truyền user với id
   → Cách checkEmailExist hoạt động: SELECT COUNT(*) FROM users 
     WHERE email = ? AND id != id hiện tại
   → Nếu count > 0 → DuplicateResourceException

4. Kiểm tra phone đã bị trùng bởi user khác chưa (nếu phone != null)

5. Cập nhật user:
   - name = request.name
   - email = request.email
   - phone = request.phone
   - role = request.role
   - Nếu có password mới → password = BCrypt.encode(newPassword)
   - updatedAt = NOW

6. Lưu: userService.updateUser(user)

7. Trả về: 200 OK
```

**Code mẫu:**

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest req) {

    // 1. Find existing user
    User existingUser = userService.getUserById(id);
    if (existingUser == null) {
        throw new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id);
    }

    // 2. Validate password match (if password provided)
    if (req.getPassword() != null && !req.getPassword().isBlank()) {
        if (!userService.comparePassword(req.getPassword(), req.getConfirmPassword())) {
            throw new BusinessValidationException("Mật khẩu xác nhận không khớp");
        }
    }

    // 3. Check email uniqueness (excluding current user)
    User emailCheckUser = new User();
    emailCheckUser.setId(id);
    emailCheckUser.setEmail(req.getEmail());
    if (userService.checkEmailExist(emailCheckUser)) {
        throw new DuplicateResourceException("Email đã được sử dụng bởi người khác: " + req.getEmail());
    }

    // 4. Check phone uniqueness (excluding current user)
    if (req.getPhone() != null && !req.getPhone().isBlank()) {
        User phoneCheckUser = new User();
        phoneCheckUser.setId(id);
        phoneCheckUser.setPhone(req.getPhone());
        if (userService.checkPhoneExist(phoneCheckUser)) {
            throw new DuplicateResourceException("Số điện thoại đã được sử dụng bởi người khác");
        }
    }

    // 5. Update fields
    existingUser.setName(req.getName());
    existingUser.setEmail(req.getEmail());
    existingUser.setPhone(req.getPhone());
    existingUser.setRole(req.getRole().toUpperCase());

    if (req.getPassword() != null && !req.getPassword().isBlank()) {
        existingUser.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    // 6. Save
    userService.updateUser(existingUser);

    return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", null));
}
```

**Exceptions:**
| Exception | HTTP | Điều kiện |
|-----------|------|-----------|
| `ResourceNotFoundException` | 404 | User id không tồn tại |
| `BusinessValidationException` | 400 | Password không khớp confirmPassword |
| `DuplicateResourceException` | 409 | Email bị trùng với user khác |
| `DuplicateResourceException` | 409 | Phone bị trùng với user khác |

---

### 1.4 DELETE /admin/users/{id} — Xóa user

**Controller:** `UserApiController.deleteUser()`
**Service:** `IUserService.deleteUser(Long id)`

**HTTP Request:**
```
DELETE /api/v1/admin/users/5
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Lấy user hiện tại đang login (currentUserResolver)
   → currentUser = currentUserResolver.getCurrentUser()

2. Tìm user cần xóa theo id
   → Nếu không tồn tại → ResourceNotFoundException

3. Kiểm tra bảo mật:
   → Nếu user cần xóa == user đang login → BusinessValidationException("Không thể xóa chính mình")
   → Nếu role của user cần xóa là ADMIN → BusinessValidationException("Không thể xóa tài khoản ADMIN")

4. Xóa user: userService.deleteUser(id)

5. Trả về: 200 OK
```

**Code mẫu:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
    // 1. Get current logged-in user
    User currentUser = currentUserResolver.getCurrentUser();

    // 2. Find user to delete
    User userToDelete = userService.getUserById(id);
    if (userToDelete == null) {
        throw new ResourceNotFoundException("Không tìm thấy người dùng với id: " + id);
    }

    // 3. Security checks
    if (currentUser.getId().equals(id)) {
        throw new BusinessValidationException("Không thể xóa chính tài khoản của bạn");
    }
    if ("ADMIN".equals(userToDelete.getRole().toUpperCase())) {
        throw new BusinessValidationException("Không thể xóa tài khoản ADMIN");
    }

    // 4. Delete
    userService.deleteUser(id);

    return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
}
```

**Exceptions:**
| Exception | HTTP | Điều kiện |
|-----------|------|-----------|
| `ResourceNotFoundException` | 404 | User id không tồn tại |
| `BusinessValidationException` | 400 | Cố xóa chính mình |
| `BusinessValidationException` | 400 | Cố xóa user ADMIN |

---

## 2. TABLE CRUD

### Mục lục Table
- [2.1 GET /admin/tables/all](#21-get-admintablesall---tất-cả-bàn)
- [2.2 GET /admin/tables](#22-get-admintables---danh-sách-bàn-phân-trang)
- [2.3 POST /admin/tables](#23-post-admintables---tạo-bàn)
- [2.4 PUT /admin/tables/{id}](#24-put-admintablesid---cập-nhật-bàn)
- [2.5 DELETE /admin/tables/{id}](#25-delete-admintablesid---xóa-bàn-soft-delete)

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
TableApiController.java           ← [1] Endpoint
    │
    ▼
ITableService                    ← [2] Interface
    │
    ▼
TableService.java                ← [3] Business logic
    │
    └──► TableRepository.java       ← [4] CRUD database
```

### 2.1 GET /admin/tables/all — Tất cả bàn

**Controller:** `TableApiController.getAllTables()`
**Service:** `ITableService.getAllTables()`

**HTTP Request:**
```
GET /api/v1/admin/tables/all
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Gọi: tableService.getAllTables()
   → SELECT * FROM tables WHERE deleted_at IS NULL ORDER BY name

2. Chuyển List<Table> → List<TableResponse>
   → tableResponses = tables.stream().map(TableResponse::fromEntity).collect(toList())

3. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping("/all")
public ResponseEntity<ApiResponse<List<TableResponse>>> getAllTables() {
    List<TableResponse> tables = tableService.getAllTables().stream()
            .map(TableResponse::fromEntity)
            .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách tất cả bàn thành công",
        tables
    ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "name": "Bàn 1",
            "capacity": 4,
            "status": "AVAILABLE",
            "location": "Tầng 1"
        }
    ],
    "timestamp": "2026-05-30T12:00:00"
}
```

---

### 2.2 GET /admin/tables — Danh sách bàn phân trang

**Controller:** `TableApiController.listTables()`
**Service:** `ITableService` — nhiều method

**HTTP Request:**
```
GET /api/v1/admin/tables?page=1&itemsPerPage=10&status=AVAILABLE&keyword=bàn+1
Authorization: Bearer <token>
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang |
| itemsPerPage | int | 10 | Số item/trang |
| status | String | null | AVAILABLE / OCCUPIED / RESERVED |
| keyword | String | null | Tìm theo name hoặc location |

**Logic cần thực hiện:**

```
1. Tính pageIndex = page - 1

2. Xác định method gọi:
   
   A. Có keyword + có status:
      → tableService.findByKeywordAndStatus(keyword, status, pageIndex, itemsPerPage)
   
   B. Có keyword, không có status:
      → tableService.findByKeyword(keyword, pageIndex, itemsPerPage)
   
   C. Có status, không có keyword:
      → tableService.getTablesByPageAndStatus(pageIndex, itemsPerPage, status)
   
   D. Không có gì:
      → tableService.getTablesByPage(pageIndex, itemsPerPage)

3. Chuyển Page<Table> → Page<TableResponse>

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<TableResponse>>> listTables(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword) {

    int pageIndex = page - 1;
    Page<TableResponse> tablePage;

    if (keyword != null && !keyword.isBlank() && status != null && !status.isBlank()) {
        tablePage = tableService.findByKeywordAndStatus(keyword, status, pageIndex, itemsPerPage)
                .map(TableResponse::fromEntity);
    } else if (keyword != null && !keyword.isBlank()) {
        tablePage = tableService.findByKeyword(keyword, pageIndex, itemsPerPage)
                .map(TableResponse::fromEntity);
    } else if (status != null && !status.isBlank()) {
        tablePage = tableService.getTablesByPageAndStatus(pageIndex, itemsPerPage, status)
                .map(TableResponse::fromEntity);
    } else {
        tablePage = tableService.getTablesByPage(pageIndex, itemsPerPage)
                .map(TableResponse::fromEntity);
    }

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách bàn thành công",
        tablePage
    ));
}
```

---

### 2.3 POST /admin/tables — Tạo bàn

**Controller:** `TableApiController.createTable()`
**Service:** `ITableService.createTable(Table table)`

**HTTP Request:**
```
POST /api/v1/admin/tables
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Bàn VIP 1",
    "capacity": 8,
    "status": "AVAILABLE",
    "location": "Tầng 2 - Khu VIP"
}
```

**Input (TableRequest):**
| Field | Ràng buộc | Mô tả |
|-------|-----------|--------|
| name | @NotBlank, max 100 | Tên bàn (duy nhất) |
| capacity | @NotNull, @Min(1), @Max(100) | Số người tối đa |
| status | @NotBlank | AVAILABLE / OCCUPIED / RESERVED |
| location | max 255 | Vị trí bàn |

**Logic cần thực hiện:**

```
1. Tạo Table entity:
   - name = request.name
   - capacity = request.capacity
   - status = request.status (AVAILABLE/OCCUPIED/RESERVED)
   - location = request.location
   - createdAt = NOW

2. Lưu: tableService.createTable(table)

3. Trả về: 201 Created + TableResponse
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<TableResponse>> createTable(@Valid @RequestBody TableRequest req) {
    Table table = new Table();
    table.setName(req.getName());
    table.setCapacity(req.getCapacity());
    table.setStatus(req.getStatus().toUpperCase());
    table.setLocation(req.getLocation());

    Table saved = tableService.createTable(table);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo bàn thành công", TableResponse.fromEntity(saved)));
}
```

---

### 2.4 PUT /admin/tables/{id} — Cập nhật bàn

**Controller:** `TableApiController.updateTable()`
**Service:** `ITableService.updateTable(Table table)`

**HTTP Request:**
```
PUT /api/v1/admin/tables/3
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Bàn VIP 1 - Updated",
    "capacity": 10,
    "status": "RESERVED",
    "location": "Tầng 2 - VIP Corner"
}
```

**Logic cần thực hiện:**

```
1. Tìm bàn cũ theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Cập nhật:
   - name = request.name
   - capacity = request.capacity
   - status = request.status
   - location = request.location
   - updatedAt = NOW

3. Lưu: tableService.updateTable(table)

4. Trả về: 200 OK + TableResponse
```

**Code mẫu:**

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<TableResponse>> updateTable(
        @PathVariable Long id,
        @Valid @RequestBody TableRequest req) {

    Table existing = tableService.getTableById(id);
    if (existing == null) {
        throw new ResourceNotFoundException("Không tìm thấy bàn với id: " + id);
    }

    existing.setName(req.getName());
    existing.setCapacity(req.getCapacity());
    existing.setStatus(req.getStatus().toUpperCase());
    existing.setLocation(req.getLocation());

    Table updated = tableService.updateTable(existing);

    return ResponseEntity.ok(ApiResponse.success(
        "Cập nhật bàn thành công",
        TableResponse.fromEntity(updated)
    ));
}
```

---

### 2.5 DELETE /admin/tables/{id} — Xóa bàn (Soft Delete)

**Controller:** `TableApiController.deleteTable()`
**Service:** `ITableService.deleteTable(Long id)`

**HTTP Request:**
```
DELETE /api/v1/admin/tables/3
Authorization: Bearer <token>
```

> **QUAN TRỌNG:** Đây là **Soft Delete** — không xóa vĩnh viễn khỏi DB.

**Logic cần thực hiện:**

```
1. Tìm bàn theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Kiểm tra bàn có reservation đang ACTIVE không
   → Tìm reservations của bàn với status != CANCELLED
   → Nếu có → BusinessValidationException("Bàn đang có reservation. Không thể xóa.")

3. Soft delete:
   → table.setDeletedAt(NOW)  // đánh dấu thời điểm xóa
   → tableService.updateTable(table)

   Hoặc gọi trực tiếp:
   → tableService.deleteTable(id)

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteTable(@PathVariable Long id) {
    Table table = tableService.getTableById(id);
    if (table == null) {
        throw new ResourceNotFoundException("Không tìm thấy bàn với id: " + id);
    }

    // Kiểm tra reservation active
    // reservationRepository.findByTableIdAndStatusNot(id, "CANCELLED")
    // → Nếu có → throw BusinessValidationException

    tableService.deleteTable(id);

    return ResponseEntity.ok(ApiResponse.success("Xóa bàn thành công", null));
}
```

**Entity Note — Soft Delete:**
```java
// Table.java
@SQLRestriction("deleted_at IS NULL")  // ← Tự động filter deleted_at != null
private Timestamp deletedAt;
```

---

## 3. FOOD CRUD

### Mục lục Food
- [3.1 GET /admin/foods](#31-get-adminfoods---danh-sách-món-ăn)
- [3.2 POST /admin/foods](#32-post-adminfoods---tạo-món-ăn)
- [3.3 PUT /admin/foods/{id}](#33-put-adminfoodsid---cập-nhật-món-ăn)
- [3.4 DELETE /admin/foods/{id}](#34-delete-adminfoodsid---xóa-món-ăn)

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
FoodApiController.java            ← [1] Endpoint
    │
    ▼
IFoodService                     ← [2] Interface
    │
    ▼
FoodService.java                 ← [3] Business logic
    │
    ├──► FoodRepository.java         ← [4] CRUD database
    ├──► ComboFoodRepository.java     ← [5] Xóa combo_foods khi xóa food
    ├──► ReservationFoodRepository.java ← [6] Xóa reservation_foods khi xóa food
    └──► CurrentUserResolver.java     ← [7] Lấy user hiện tại (createdBy)
```

### 3.1 GET /admin/foods — Danh sách món ăn

**Controller:** `FoodApiController.listFoods()`
**Service:** `IFoodService` — nhiều method

**HTTP Request:**
```
GET /api/v1/admin/foods?page=1&itemsPerPage=10&status=AVAILABLE&mealType=LUNCH&keyword=gà
Authorization: Bearer <token>
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang |
| itemsPerPage | int | 10 | Số item/trang |
| status | String | null | AVAILABLE / UNAVAILABLE |
| mealType | String | null | BREAKFAST / LUNCH / DINNER / DESSERT |
| keyword | String | null | Tìm theo tên / description |

**Logic cần thực hiện:**

```
1. Tính pageIndex = page - 1

2. Xác định method gọi dựa trên filter:
   
   A. Có keyword + có status + có mealType:
      → foodService.findByKeywordAndStatusAndMealType(keyword, status, mealType, pageIndex, itemsPerPage)
   
   B. Có keyword + có status:
      → foodService.findByKeywordAndStatus(keyword, status, pageIndex, itemsPerPage)
   
   C. Có keyword + có mealType:
      → foodService.findByKeywordAndMealType(keyword, mealType, pageIndex, itemsPerPage)
   
   D. Có keyword:
      → foodService.findByKeyword(keyword, pageIndex, itemsPerPage)
   
   E. Có status + có mealType:
      → foodService.getFoodsByStatusAndMealType(pageIndex, itemsPerPage, status, mealType)
   
   F. Có status:
      → foodService.getFoodsByStatus(pageIndex, itemsPerPage, status)
   
   G. Có mealType:
      → foodService.getFoodsByMealType(pageIndex, itemsPerPage, mealType)
   
   H. Không có gì:
      → foodService.getFoods(pageIndex, itemsPerPage)

3. Chuyển Page<Food> → Page<FoodResponse>

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping
@Transactional(readOnly = true)
public ResponseEntity<ApiResponse<Page<FoodResponse>>> listFoods(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "mealType", required = false) String mealType,
        @RequestParam(value = "keyword", required = false) String keyword) {

    int pageIndex = page - 1;
    Page<FoodResponse> foodPage;

    boolean hasStatus = status != null && !status.isBlank();
    boolean hasMealType = mealType != null && !mealType.isBlank();
    boolean hasKeyword = keyword != null && !keyword.isBlank();

    if (hasKeyword && hasStatus && hasMealType) {
        foodPage = foodService.findByKeywordAndStatusAndMealType(
                keyword, status, mealType, pageIndex, itemsPerPage).map(FoodResponse::fromEntity);
    } else if (hasKeyword && hasStatus) {
        foodPage = foodService.findByKeywordAndStatus(
                keyword, status, pageIndex, itemsPerPage).map(FoodResponse::fromEntity);
    } else if (hasKeyword && hasMealType) {
        foodPage = foodService.findByKeywordAndMealType(
                keyword, mealType, pageIndex, itemsPerPage).map(FoodResponse::fromEntity);
    } else if (hasKeyword) {
        foodPage = foodService.findByKeyword(keyword, pageIndex, itemsPerPage)
                .map(FoodResponse::fromEntity);
    } else if (hasStatus && hasMealType) {
        foodPage = foodService.getFoodsByStatusAndMealType(
                pageIndex, itemsPerPage, status, mealType).map(FoodResponse::fromEntity);
    } else if (hasStatus) {
        foodPage = foodService.getFoodsByStatus(pageIndex, itemsPerPage, status)
                .map(FoodResponse::fromEntity);
    } else if (hasMealType) {
        foodPage = foodService.getFoodsByMealType(pageIndex, itemsPerPage, mealType)
                .map(FoodResponse::fromEntity);
    } else {
        foodPage = foodService.getFoods(pageIndex, itemsPerPage)
                .map(FoodResponse::fromEntity);
    }

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách món ăn thành công",
        foodPage
    ));
}
```

---

### 3.2 POST /admin/foods — Tạo món ăn

**Controller:** `FoodApiController.createFood()`
**Service:** `IFoodService.createFood(Food food)`

**HTTP Request:**
```
POST /api/v1/admin/foods
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Gà Chiên Giòn",
    "description": "Gà chiên giòn rụm, giòn rụm",
    "price": 89000,
    "imageUrl": "https://example.com/images/ga-chien.jpg",
    "status": "AVAILABLE",
    "mealType": "LUNCH"
}
```

**Input (FoodRequest):**
| Field | Ràng buộc | Mô tả |
|-------|-----------|--------|
| name | @NotBlank, max 255 | Tên món ăn |
| description | max 500 | Mô tả |
| price | @NotNull, @Min(0) | Giá (VNĐ) |
| imageUrl | String | URL hình ảnh |
| status | @NotBlank | AVAILABLE / UNAVAILABLE |
| mealType | @NotBlank | BREAKFAST / LUNCH / DINNER / DESSERT |

**Logic cần thực hiện:**

```
1. Lấy user hiện tại (admin đang login)
   → User currentUser = currentUserResolver.getCurrentUser()

2. Tạo Food entity:
   - name = request.name
   - description = request.description
   - price = request.price
   - imageUrl = request.imageUrl
   - status = request.status (AVAILABLE/UNAVAILABLE)
   - mealType = request.mealType (BREAKFAST/LUNCH/DINNER/DESSERT)
   - createdBy = currentUser  // ← quan trọng: biết ai tạo
   - createdAt = NOW

3. Lưu: foodService.createFood(food)

4. Trả về: 201 Created + FoodResponse
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<FoodResponse>> createFood(@Valid @RequestBody FoodRequest req) {
    // 1. Get current user
    User currentUser = currentUserResolver.getCurrentUser();

    // 2. Build entity
    Food food = new Food();
    food.setName(req.getName());
    food.setDescription(req.getDescription());
    food.setPrice(req.getPrice());
    food.setImageUrl(req.getImageUrl());
    food.setStatus(req.getStatus().toUpperCase());
    food.setMealType(req.getMealType().toUpperCase());
    food.setCreatedBy(currentUser);

    // 3. Save
    Food saved = foodService.createFood(food);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo món ăn thành công", FoodResponse.fromEntity(saved)));
}
```

---

### 3.3 PUT /admin/foods/{id} — Cập nhật món ăn

**Controller:** `FoodApiController.updateFood()`
**Service:** `IFoodService.updateFood(Food food)`

**HTTP Request:**
```
PUT /api/v1/admin/foods/5
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Gà Chiên Giòn - Phiên bản đặc biệt",
    "description": "Gà chiên giòn rụm, sốt đặc biệt",
    "price": 99000,
    "imageUrl": "https://example.com/images/ga-chien-v2.jpg",
    "status": "AVAILABLE",
    "mealType": "DINNER"
}
```

**Logic cần thực hiện:**

```
1. Tìm food cũ theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Cập nhật:
   - name = request.name
   - description = request.description
   - price = request.price
   - imageUrl = request.imageUrl
   - status = request.status
   - mealType = request.mealType
   - updatedAt = NOW
   // createdBy giữ nguyên (không thay đổi)

3. Lưu: foodService.updateFood(food)

4. Trả về: 200 OK + FoodResponse
```

**Code mẫu:**

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<FoodResponse>> updateFood(
        @PathVariable Long id,
        @Valid @RequestBody FoodRequest req) {

    Food existing = foodService.getFoodById(id);
    if (existing == null) {
        throw new ResourceNotFoundException("Không tìm thấy món ăn với id: " + id);
    }

    existing.setName(req.getName());
    existing.setDescription(req.getDescription());
    existing.setPrice(req.getPrice());
    existing.setImageUrl(req.getImageUrl());
    existing.setStatus(req.getStatus().toUpperCase());
    existing.setMealType(req.getMealType().toUpperCase());

    Food updated = foodService.updateFood(existing);

    return ResponseEntity.ok(ApiResponse.success(
        "Cập nhật món ăn thành công",
        FoodResponse.fromEntity(updated)
    ));
}
```

---

### 3.4 DELETE /admin/foods/{id} — Xóa món ăn

**Controller:** `FoodApiController.deleteFood()`
**Service:** `IFoodService.deleteFood(Long id)`

**HTTP Request:**
```
DELETE /api/v1/admin/foods/5
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Tìm food theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Xóa cascade (rất quan trọng):
   a. Xóa combo_foods liên quan:
      → comboFoodRepository.deleteByFoodId(id)
      → DELETE FROM combo_foods WHERE food_id = ?
   
   b. Xóa reservation_foods liên quan:
      → reservationFoodRepository.deleteByFoodId(id)
      → DELETE FROM reservation_foods WHERE food_id = ?
   
   c. Xóa food:
      → foodService.deleteFood(id)
      → DELETE FROM foods WHERE id = ?

3. Trả về: 200 OK
```

**Code mẫu:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteFood(@PathVariable Long id) {
    Food food = foodService.getFoodById(id);
    if (food == null) {
        throw new ResourceNotFoundException("Không tìm thấy món ăn với id: " + id);
    }

    // Cascade delete - xóa tất cả dữ liệu liên quan trước
    comboFoodRepository.deleteByFoodId(id);
    reservationFoodRepository.deleteByFoodId(id);

    // Xóa food
    foodService.deleteFood(id);

    return ResponseEntity.ok(ApiResponse.success("Xóa món ăn thành công", null));
}
```

> **LƯU Ý:** Cần `@Transactional` để đảm bảo tất cả xóa thành công hoặc rollback nếu lỗi.

---

## 4. COMBO CRUD

### Mục lục Combo
- [4.1 GET /admin/combos](#41-get-admincombos---danh-sách-combo)
- [4.2 GET /admin/combos/{id}](#42-get-admincombosid---chi-tiết-combo)
- [4.3 POST /admin/combos](#43-post-admincombos---tạo-combo)
- [4.4 PUT /admin/combos/{id}](#44-put-admincombosid---cập-nhật-combo)
- [4.5 DELETE /admin/combos/{id}](#45-delete-admincombosid---xóa-combo)

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
ComboApiController.java           ← [1] Endpoint
    │
    ▼
IComboService                    ← [2] Interface
    │
    ▼
ComboService.java                ← [3] Business logic
    │
    ├──► ComboRepository.java         ← [4] CRUD combo
    ├──► ComboFoodRepository.java     ← [5] CRUD combo_foods
    ├──► FoodRepository.java         ← [6] Tìm food để thêm vào combo
    └──► ReservationComboRepository.java ← [7] Kiểm tra reservation trước xóa
```

### 4.1 GET /admin/combos — Danh sách combo

**Controller:** `ComboApiController.listCombos()`
**Service:** `IComboService` — nhiều method

**HTTP Request:**
```
GET /api/v1/admin/combos?page=1&itemsPerPage=10&status=available&keyword=combo+set
Authorization: Bearer <token>
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang |
| itemsPerPage | int | 10 | Số item/trang |
| status | String | null | available / unavailable |
| keyword | String | null | Tìm theo name / description |

**Logic cần thực hiện:**

```
1. Tính pageIndex = page - 1

2. Xác định method gọi:
   
   A. Có keyword + có status:
      → comboService.findByKeywordAndStatus(keyword, status, pageIndex, itemsPerPage)
   
   B. Có keyword:
      → comboService.findByKeyword(keyword, pageIndex, itemsPerPage)
   
   C. Có status:
      → comboService.getCombosByPageAndStatus(pageIndex, itemsPerPage, status)
   
   D. Không có gì:
      → comboService.getCombosByPage(pageIndex, itemsPerPage)

3. Chuyển Page<Combo> → Page<ComboResponse>
   → Lưu ý: ComboResponse.fromEntity(c, comboFoods) cần truyền comboFoods

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ComboResponse>>> listCombos(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword) {

    int pageIndex = page - 1;
    Page<ComboResponse> comboPage;

    if (keyword != null && !keyword.isBlank() && status != null && !status.isBlank()) {
        comboPage = comboService.findByKeywordAndStatus(keyword, status, pageIndex, itemsPerPage)
                .map(combo -> {
                    List<ComboFood> comboFoods = comboService.getComboFoods(combo.getId());
                    return ComboResponse.fromEntity(combo, comboFoods);
                });
    } else if (keyword != null && !keyword.isBlank()) {
        comboPage = comboService.findByKeyword(keyword, pageIndex, itemsPerPage)
                .map(combo -> {
                    List<ComboFood> comboFoods = comboService.getComboFoods(combo.getId());
                    return ComboResponse.fromEntity(combo, comboFoods);
                });
    } else if (status != null && !status.isBlank()) {
        comboPage = comboService.getCombosByPageAndStatus(pageIndex, itemsPerPage, status)
                .map(combo -> {
                    List<ComboFood> comboFoods = comboService.getComboFoods(combo.getId());
                    return ComboResponse.fromEntity(combo, comboFoods);
                });
    } else {
        comboPage = comboService.getCombosByPage(pageIndex, itemsPerPage)
                .map(combo -> {
                    List<ComboFood> comboFoods = comboService.getComboFoods(combo.getId());
                    return ComboResponse.fromEntity(combo, comboFoods);
                });
    }

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách combo thành công",
        comboPage
    ));
}
```

---

### 4.2 GET /admin/combos/{id} — Chi tiết combo

**Controller:** `ComboApiController.getCombo()`
**Service:** `IComboService.getComboById()` + `getComboFoods()`

**HTTP Request:**
```
GET /api/v1/admin/combos/3
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Tìm combo theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Lấy combo_foods:
   → comboFoods = comboService.getComboFoods(id)
   → comboFoodRepository.findByComboId(id)
   → SELECT * FROM combo_foods WHERE combo_id = ?

3. Trả về: 200 OK + ComboResponse.fromEntity(combo, comboFoods)
```

**Code mẫu:**

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ComboResponse>> getCombo(@PathVariable Long id) {
    Combo combo = comboService.getComboById(id);
    if (combo == null) {
        throw new ResourceNotFoundException("Không tìm thấy combo với id: " + id);
    }

    List<ComboFood> comboFoods = comboService.getComboFoods(id);

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy chi tiết combo thành công",
        ComboResponse.fromEntity(combo, comboFoods)
    ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "data": {
        "id": 3,
        "name": "Combo Trưa Văn Phòng",
        "price": 159000,
        "description": "Combo tiết kiệm cho buổi trưa",
        "status": "available",
        "imageUrl": "https://example.com/combo-lunch.jpg",
        "foodItems": [
            { "foodId": 1, "quantity": 1 },
            { "foodId": 3, "quantity": 2 }
        ]
    },
    "timestamp": "2026-05-30T12:00:00"
}
```

---

### 4.3 POST /admin/combos — Tạo combo

**Controller:** `ComboApiController.createCombo()`
**Service:** `IComboService.createComboWithFoods(ComboRequest request)`

**HTTP Request:**
```
POST /api/v1/admin/combos
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Combo Trưa Văn Phòng",
    "price": 159000,
    "description": "Combo tiết kiệm cho buổi trưa",
    "status": "available",
    "imageUrl": "https://example.com/combo-lunch.jpg",
    "foodItems": [
        { "foodId": 1, "quantity": 1 },
        { "foodId": 3, "quantity": 2 }
    ]
}
```

**Input (ComboRequest):**
| Field | Ràng buộc | Mô tả |
|-------|-----------|--------|
| name | @NotBlank, max 255 | Tên combo |
| price | @NotNull, @Min(0) | Giá combo (VNĐ) |
| description | max 500 | Mô tả |
| status | String | available / unavailable (default: available) |
| imageUrl | String | URL hình ảnh |
| foodItems | List<ComboFoodDto> | Danh sách món ăn trong combo |

**Logic cần thực hiện:**

```
1. Tạo Combo entity:
   - name = request.name
   - price = request.price
   - description = request.description
   - status = request.status (default: "available")
   - imageUrl = request.imageUrl
   - createdAt = NOW

2. Lưu Combo trước (để lấy id):
   → savedCombo = comboRepository.save(combo)

3. Với mỗi ComboFoodDto trong foodItems:
   a. Tìm Food theo foodId
      → food = foodRepository.findById(foodId)
      → Nếu không tìm thấy → ResourceNotFoundException
   b. Tạo ComboFood entity:
      - combo = savedCombo
      - food = food
      - quantity = dto.quantity
   c. Lưu: comboFoodRepository.save(comboFood)

4. Lấy lại danh sách comboFoods vừa tạo:
   → comboFoods = comboFoodRepository.findByComboId(savedCombo.getId())

5. Trả về: 201 Created + ComboResponse.fromEntity(savedCombo, comboFoods)
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<ComboResponse>> createCombo(@RequestBody ComboRequest request) {
    // 1. Build combo entity
    Combo combo = new Combo();
    combo.setName(request.getName());
    combo.setPrice(request.getPrice());
    combo.setDescription(request.getDescription());
    combo.setStatus(request.getStatus() != null ? request.getStatus().toLowerCase() : "available");
    combo.setImageUrl(request.getImageUrl());

    // 2. Save combo first (get id)
    Combo savedCombo = comboService.createCombo(combo);

    // 3. Save combo_foods
    List<ComboFoodDto> savedFoodItems = new ArrayList<>();
    if (request.getFoodItems() != null && !request.getFoodItems().isEmpty()) {
        for (ComboFoodDto dto : request.getFoodItems()) {
            Food food = foodRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy món ăn với id: " + dto.getFoodId()));

            ComboFood comboFood = new ComboFood();
            comboFood.setCombo(savedCombo);
            comboFood.setFood(food);
            comboFood.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);

            comboFoodRepository.save(comboFood);
            savedFoodItems.add(dto);
        }
    }

    // 4. Get saved combo_foods for response
    List<ComboFood> comboFoods = comboFoodRepository.findByComboId(savedCombo.getId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Tạo combo thành công",
            ComboResponse.fromEntity(savedCombo, comboFoods)
        ));
}
```

---

### 4.4 PUT /admin/combos/{id} — Cập nhật combo

**Controller:** `ComboApiController.updateCombo()`
**Service:** `IComboService.updateComboWithFoods(Long id, ComboRequest request)`

**HTTP Request:**
```
PUT /api/v1/admin/combos/3
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Combo Trưa Văn Phòng - Updated",
    "price": 169000,
    "description": "Combo tiết kiệm cho buổi trưa - phiên bản mới",
    "status": "available",
    "imageUrl": "https://example.com/combo-lunch-v2.jpg",
    "foodItems": [
        { "foodId": 1, "quantity": 1 },
        { "foodId": 3, "quantity": 1 },
        { "foodId": 5, "quantity": 2 }
    ]
}
```

**Logic cần thực hiện:**

```
1. Tìm combo cũ theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Cập nhật thông tin combo:
   - name = request.name
   - price = request.price
   - description = request.description
   - status = request.status
   - imageUrl = request.imageUrl
   - updatedAt = NOW

3. XÓA TẤT CẢ combo_foods cũ:
   → comboFoodRepository.deleteByComboId(id)
   → DELETE FROM combo_foods WHERE combo_id = ?

4. TẠO LẠI combo_foods mới:
   (giống bước 3 của POST)

5. Lưu combo đã cập nhật: comboRepository.save(combo)

6. Lấy danh sách combo_foods mới:
   → comboFoods = comboFoodRepository.findByComboId(id)

7. Trả về: 200 OK + ComboResponse
```

**Code mẫu:**

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ComboResponse>> updateCombo(
        @PathVariable Long id,
        @RequestBody ComboRequest request) {

    Combo existing = comboService.getComboById(id);
    if (existing == null) {
        throw new ResourceNotFoundException("Không tìm thấy combo với id: " + id);
    }

    // 1. Update combo info
    existing.setName(request.getName());
    existing.setPrice(request.getPrice());
    existing.setDescription(request.getDescription());
    existing.setStatus(request.getStatus() != null ? request.getStatus().toLowerCase() : "available");
    existing.setImageUrl(request.getImageUrl());

    // 2. Delete all old combo_foods
    comboFoodRepository.deleteByComboId(id);

    // 3. Create new combo_foods
    if (request.getFoodItems() != null && !request.getFoodItems().isEmpty()) {
        for (ComboFoodDto dto : request.getFoodItems()) {
            Food food = foodRepository.findById(dto.getFoodId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy món ăn với id: " + dto.getFoodId()));

            ComboFood comboFood = new ComboFood();
            comboFood.setCombo(existing);
            comboFood.setFood(food);
            comboFood.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);

            comboFoodRepository.save(comboFood);
        }
    }

    // 4. Save updated combo
    Combo savedCombo = comboService.updateCombo(existing);

    // 5. Get new combo_foods for response
    List<ComboFood> comboFoods = comboFoodRepository.findByComboId(id);

    return ResponseEntity.ok(ApiResponse.success(
        "Cập nhật combo thành công",
        ComboResponse.fromEntity(savedCombo, comboFoods)
    ));
}
```

---

### 4.5 DELETE /admin/combos/{id} — Xóa combo

**Controller:** `ComboApiController.deleteCombo()`
**Service:** `IComboService.deleteCombo(Long id)`

**HTTP Request:**
```
DELETE /api/v1/admin/combos/3
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Tìm combo theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Kiểm tra combo có trong reservation_combos không:
   → reservationComboRepository.existsByComboId(id)
   → SELECT COUNT(*) FROM reservation_combos WHERE combo_id = ? AND reservation.status != CANCELLED
   → Nếu có → BusinessValidationException("Combo đang có trong reservation. Không thể xóa.")

3. Xóa cascade:
   a. Xóa combo_foods: comboFoodRepository.deleteByComboId(id)
   b. Xóa combo: comboService.deleteCombo(id)

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteCombo(@PathVariable Long id) {
    Combo combo = comboService.getComboById(id);
    if (combo == null) {
        throw new ResourceNotFoundException("Không tìm thấy combo với id: " + id);
    }

    // Check if combo is in any active reservation
    if (reservationComboRepository.existsByComboId(id)) {
        throw new BusinessValidationException(
            "Combo đang có trong đơn đặt hàng. Không thể xóa.");
    }

    // Cascade delete
    comboFoodRepository.deleteByComboId(id);
    comboService.deleteCombo(id);

    return ResponseEntity.ok(ApiResponse.success("Xóa combo thành công", null));
}
```

---

## 5. RESERVATION CRUD

### Mục lục Reservation
- [5.1 GET /admin/reservations](#51-get-adminreservations---danh-sách-reservation)
- [5.2 POST /admin/reservations](#52-post-adminreservations---tạo-reservation-thủ-công)
- [5.3 DELETE /admin/reservations/{id}](#53-delete-adminreservationsid---hủy-reservation)
- [5.4 PUT /admin/reservations/{id}/assign-table](#54-put-adminreservationsidassign-table---gán-bàn)

### Các file tham gia

```
REQUEST HTTP
    │
    ▼
ReservationApiController.java     ← [1] Endpoint
    │
    ▼
IReservationService              ← [2] Interface
    │
    ▼
ReservationService.java          ← [3] Business logic
    │
    ├──► ReservationRepository.java     ← [4] CRUD reservation
    ├──► TableRepository.java           ← [5] Tìm/gán bàn
    ├──► UserRepository.java            ← [6] Tìm customer
    ├──► ReservationTableRepository.java ← [7] CRUD reservation_tables
    └──► FoodRepository.java / ComboRepository.java ← [8] Tính giá
```

### 5.1 GET /admin/reservations — Danh sách reservation

**Controller:** `ReservationApiController.listReservations()`
**Service:** `IReservationService.searchReservations()`

**HTTP Request:**
```
GET /api/v1/admin/reservations?page=1&itemsPerPage=10&status=PENDING&keyword=nguyen&date=2026-05-30
Authorization: Bearer <token>
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang |
| itemsPerPage | int | 10 | Số item/trang |
| status | String | null | PENDING / CONFIRMED / CANCELLED / COMPLETED |
| keyword | String | null | Tìm theo customer name/phone/note |
| date | String | null | Filter theo ngày (yyyy-MM-dd) |

**Logic cần thực hiện:**

```
1. Tính pageIndex = page - 1

2. Gọi: reservationService.searchReservations(keyword, status, date, pageIndex, itemsPerPage)
   → Tổng hợp cả 4 filter trong 1 method

3. Chuyển Page<Reservation> → Page<ReservationResponse>
   → reservationResponsePage.map(ReservationResponse::fromEntity)

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ReservationResponse>>> listReservations(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "date", required = false) String date) {

    int pageIndex = page - 1;

    Page<ReservationResponse> reservationPage = reservationService
        .searchReservations(keyword, status, date, pageIndex, itemsPerPage)
        .map(ReservationResponse::fromEntity);

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách reservation thành công",
        reservationPage
    ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "data": {
        "content": [
            {
                "id": 1,
                "totalPeople": 4,
                "status": "PENDING",
                "reservationAt": "2026-05-30T18:00:00",
                "note": "Khách hàng ăn chay",
                "totalPrice": 350000,
                "createdAt": "2026-05-30T12:00:00",
                "customerName": "Trần Thị C",
                "customerPhone": "0909876543",
                "tableName": null,
                "tableId": null
            }
        ],
        "totalElements": 25,
        "totalPages": 3,
        "size": 10
    }
}
```

---

### 5.2 POST /admin/reservations — Tạo reservation thủ công

**Controller:** `ReservationApiController.createReservation()`
**Service:** `IReservationService.createReservation(...)`

**HTTP Request:**
```
POST /api/v1/admin/reservations
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Trần Thị C",
    "email": "tranthic@email.com",
    "phone": "0909876543",
    "date": "2026-05-30",
    "time": "18:00",
    "numberOfPeople": 4,
    "orderDetails": "Bữa trưa văn phòng",
    "note": "Khách hàng ăn chay",
    "orderType": "food"
}
```

**Input (CreateReservationRequest):**
| Field | Ràng buộc | Mô tả |
|-------|-----------|--------|
| name | String | Tên khách hàng |
| email | @Email | Email khách |
| phone | String | SĐT khách |
| date | String | Ngày đặt (yyyy-MM-dd) |
| time | String | Giờ đặt (HH:mm) |
| numberOfPeople | @Min(1), default 2 | Số người |
| orderDetails | String | Chi tiết món ăn/order |
| note | String | Ghi chú |
| orderType | String | "food" hoặc "combo" |
| reservationAt | String | Thời điểm đặt (tự động tính từ date + time) |

**Logic cần thực hiện:**

```
1. Validate các trường bắt buộc
   → Nếu thiếu thông tin cơ bản → BusinessValidationException

2. Resolve reservationAt:
   → Nếu date + time có → parse thành Timestamp
   → reservationAt = Timestamp.valueOf(LocalDateTime.of(date, time))

3. Tìm hoặc tạo User (customer):
   → Nếu có email → tìm user theo email
   → Nếu không có → tạo guest user tạm (name + phone)

4. Tạo reservation entity:
   - customer = user tìm được/tạo
   - totalPeople = numberOfPeople
   - reservationAt = timestamp đã parse
   - orderDetails = orderDetails
   - note = note
   - status = "PENDING"
   - orderId = UUID.randomUUID().toString() (mã đơn hàng duy nhất)
   - totalPrice = 0 (tạm, tính sau nếu cần)

5. Lưu: reservationRepository.save(reservation)

6. Trả về: 201 Created
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<Void>> createReservation(
        @Valid @RequestBody CreateReservationRequest req) {

    // 1. Validate
    if (req.getName() == null || req.getName().isBlank()) {
        throw new BusinessValidationException("Tên khách hàng không được để trống");
    }

    // 2. Resolve reservationAt from date + time
    LocalDateTime reservationDateTime = null;
    if (req.getDate() != null && !req.getDate().isBlank() &&
        req.getTime() != null && !req.getTime().isBlank()) {
        LocalDate date = LocalDate.parse(req.getDate());
        LocalTime time = LocalTime.parse(req.getTime());
        reservationDateTime = LocalDateTime.of(date, time);
    }

    // 3. Find or create customer
    User customer = null;
    if (req.getEmail() != null && !req.getEmail().isBlank()) {
        customer = userRepository.findByEmail(req.getEmail());
    }

    // 4. Build reservation entity
    Reservation reservation = new Reservation();
    reservation.setCustomer(customer);
    reservation.setTotalPeople(req.getNumberOfPeople() > 0 ? req.getNumberOfPeople() : 2);
    reservation.setReservationAt(reservationDateTime != null ?
        Timestamp.valueOf(reservationDateTime) : null);
    reservation.setOrderDetails(req.getOrderDetails());
    reservation.setNote(req.getNote());
    reservation.setStatus("PENDING");
    reservation.setOrderId(UUID.randomUUID().toString());

    // 5. Save
    reservationService.createReservation(
        reservation.getCustomer() != null ? reservation.getCustomer().getName() : req.getName(),
        req.getEmail(),
        req.getPhone(),
        req.getDate(),
        req.getTime(),
        req.getNumberOfPeople(),
        req.getOrderDetails(),
        req.getOrderType(),
        reservation.getOrderId()
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo reservation thành công", null));
}
```

---

### 5.3 DELETE /admin/reservations/{id} — Hủy reservation

**Controller:** `ReservationApiController.cancelReservation()`
**Service:** `IReservationService.deleteReservationById(Long id)`

**HTTP Request:**
```
DELETE /api/v1/admin/reservations/5
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Tìm reservation theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Kiểm tra trạng thái:
   → Nếu status == CANCELLED → BusinessValidationException("Reservation đã bị hủy trước đó")
   → Nếu status == COMPLETED → BusinessValidationException("Không thể hủy reservation đã hoàn thành")

3. Cập nhật status = "CANCELLED":
   → reservation.setStatus("CANCELLED")
   → reservationRepository.save(reservation)

   Hoặc xóa hoàn toàn:
   → reservationService.deleteReservationById(id)

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> cancelReservation(@PathVariable Long id) {
    Reservation reservation = reservationService.getReservationById(id);
    if (reservation == null) {
        throw new ResourceNotFoundException("Không tìm thấy reservation với id: " + id);
    }

    if ("CANCELLED".equals(reservation.getStatus())) {
        throw new BusinessValidationException("Reservation đã bị hủy trước đó");
    }
    if ("COMPLETED".equals(reservation.getStatus())) {
        throw new BusinessValidationException("Không thể hủy reservation đã hoàn thành");
    }

    reservation.setStatus("CANCELLED");
    reservationService.deleteReservationById(id); // hoặc repository.save(reservation)

    return ResponseEntity.ok(ApiResponse.success("Hủy reservation thành công", null));
}
```

---

### 5.4 PUT /admin/reservations/{id}/assign-table — Gán bàn

**Controller:** `ReservationApiController.assignTable()`
**Service:** `IReservationService.assignTable(Long reservationId, Long tableId)`

**HTTP Request:**
```
PUT /api/v1/admin/reservations/5/assign-table
Authorization: Bearer <token>
Content-Type: application/json

{
    "tableId": 3
}
```

**Logic cần thực hiện:**

```
1. Tìm reservation theo id
   → Nếu không tồn tại → ResourceNotFoundException

2. Tìm table theo id
   → Nếu không tồn tại → ResourceNotFoundException

3. Kiểm tra trạng thái bàn:
   → Nếu table.status != AVAILABLE → BusinessValidationException("Bàn không trong trạng thái AVAILABLE")

4. Kiểm tra bàn có bị trùng lịch không:
   → Tìm các reservation khác của bàn này có thời gian overlap
   → overlap = (existingReservation.reservationAt <= reservation.reservationAt + 2h)
             AND (existingReservation.reservationAt + 2h > reservation.reservationAt)
   → Nếu có reservation ACTIVE (không phải CANCELLED) → BusinessValidationException("Bàn đã có reservation trong khung giờ này")

5. Gán bàn:
   → reservation.setTable(table)
   → table.setStatus("RESERVED")
   → reservationRepository.save(reservation)
   → tableRepository.save(table)

6. Trả về: 200 OK
```

**Code mẫu:**

```java
@PutMapping("/{id}/assign-table")
public ResponseEntity<ApiResponse<Void>> assignTable(
        @PathVariable Long id,
        @RequestBody Map<String, Long> payload) {

    Long tableId = payload.get("tableId");
    if (tableId == null) {
        throw new BusinessValidationException("tableId không được để trống");
    }

    Reservation reservation = reservationService.getReservationById(id);
    if (reservation == null) {
        throw new ResourceNotFoundException("Không tìm thấy reservation với id: " + id);
    }

    Table table = tableService.getTableById(tableId);
    if (table == null) {
        throw new ResourceNotFoundException("Không tìm thấy bàn với id: " + tableId);
    }

    if (!"AVAILABLE".equals(table.getStatus().toUpperCase())) {
        throw new BusinessValidationException("Bàn không trong trạng thái AVAILABLE");
    }

    // Check for schedule conflict
    List<Table> conflictingTables = tableRepository
        .findAvailableTableIds(
            reservation.getTotalPeople(),
            reservation.getReservationAt(),
            Timestamp.valueOf(reservation.getReservationAt().toLocalDateTime().plusHours(2))
        );
    
    boolean hasConflict = conflictingTables.stream()
        .anyMatch(t -> t.getId().equals(tableId));
    
    if (hasConflict) {
        throw new BusinessValidationException("Bàn đã có reservation trong khung giờ này");
    }

    // Assign table
    reservation.setTable(table);
    table.setStatus("RESERVED");
    reservationService.assignTable(id, tableId);

    return ResponseEntity.ok(ApiResponse.success("Gán bàn thành công", null));
}
```

---

## 6. CLIENT APIs

### Mục lục Client
- [6.1 GET /client/foods](#61-get-clientfoods---danh-sách-món-ăn-public)
- [6.2 GET /client/combos](#62-get-clientcombos---danh-sách-combo-public)
- [6.3 GET /client/tables/availability](#63-get-clienttablesavailability---kiểm-tra-bàn-trống)
- [6.4 POST /client/reservations](#64-post-clientreservations---khách-hàng-đặt-bàn)

### 6.1 GET /client/foods — Danh sách món ăn public

**Controller:** `ClientFoodApiController.listFoods()`
**Service:** `IFoodService`

**HTTP Request:**
```
GET /api/v1/client/foods?page=1&itemsPerPage=6&mealType=LUNCH
```

**Parameters:**
| Param | Kiểu | Mặc định | Mô tả |
|-------|------|-----------|--------|
| page | int | 1 | Trang |
| itemsPerPage | int | 6 | Số item/trang |
| mealType | String | null | BREAKFAST / LUNCH / DINNER / DESSERT |

> **Đặc điểm:** Không filter theo status (luôn trả AVAILABLE) và không cần JWT.

**Logic cần thực hiện:**

```
1. Lấy tất cả món ăn theo filter:
   → Có mealType → foodService.getFoodsByMealType(pageIndex, itemsPerPage, mealType)
   → Không có → foodService.getFoods(pageIndex, itemsPerPage)

2. Filter chỉ lấy AVAILABLE:
   → foods = foods.filter(f -> "AVAILABLE".equals(f.getStatus()))

3. Chuyển → List<FoodResponse> (KHÔNG phải Page)

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<List<FoodResponse>>> listFoods(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "6") int itemsPerPage,
        @RequestParam(value = "mealType", required = false) String mealType) {

    int pageIndex = page - 1;

    Page<Food> foodPage;
    if (mealType != null && !mealType.isBlank()) {
        foodPage = foodService.getFoodsByMealType(pageIndex, itemsPerPage, mealType.toUpperCase());
    } else {
        foodPage = foodService.getFoods(pageIndex, itemsPerPage);
    }

    // Filter chỉ AVAILABLE cho client
    List<FoodResponse> foods = foodPage.getContent().stream()
            .filter(f -> "AVAILABLE".equals(f.getStatus()))
            .map(FoodResponse::fromEntity)
            .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách món ăn thành công",
        foods
    ));
}
```

---

### 6.2 GET /client/combos — Danh sách combo public

**Controller:** `ClientComboApiController.listActiveCombos()`
**Service:** `IComboService`

**HTTP Request:**
```
GET /api/v1/client/combos?page=1&itemsPerPage=12
```

**Logic cần thực hiện:**

```
1. Lấy danh sách combo theo trang:
   → comboService.getCombosByPage(pageIndex, itemsPerPage)

2. Filter chỉ lấy status = "available"

3. Trả về: 200 OK (Page)
```

**Code mẫu:**

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ComboResponse>>> listActiveCombos(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "itemsPerPage", defaultValue = "12") int itemsPerPage) {

    int pageIndex = page - 1;

    Page<ComboResponse> comboPage = comboService
        .getCombosByPage(pageIndex, itemsPerPage)
        .filter(combo -> "available".equals(combo.getStatus()))
        .map(combo -> {
            List<ComboFood> comboFoods = comboService.getComboFoods(combo.getId());
            return ComboResponse.fromEntity(combo, comboFoods);
        });

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy danh sách combo thành công",
        comboPage
    ));
}
```

---

### 6.3 GET /client/tables/availability — Kiểm tra bàn trống

**Controller:** `ClientTableApiController.getAvailability()`
**Service:** `ITableService` + `TableRepository`

**HTTP Request:**
```
GET /api/v1/client/tables/availability?date=2026-05-30&time=18:00
```

**Parameters:**
| Param | Kiểu | Bắt buộc | Mô tả |
|-------|------|-----------|--------|
| date | String | Có | Ngày cần kiểm tra (yyyy-MM-dd) |
| time | String | Có | Giờ cần kiểm tra (HH:mm) |

**Logic cần thực hiện:**

```
1. Parse date + time → LocalDateTime reservationDateTime

2. Tính thời điểm kết thúc = reservationDateTime + 2 giờ

3. Tìm tất cả bàn AVAILABLE:
   → tableRepository.findByStatus("AVAILABLE")

4. Tính tổng số chỗ trống:
   → totalAvailableSeats = SUM(capacity) của các bàn AVAILABLE

5. Trả về data:
   - availableSeats: tổng số chỗ trống
   - availableTables: số bàn trống
   - date: ngày yêu cầu
   - time: giờ yêu cầu
```

**Code mẫu:**

```java
@GetMapping("/availability")
public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailability(
        @RequestParam String date,
        @RequestParam String time) {

    LocalDate localDate = LocalDate.parse(date);
    LocalTime localTime = LocalTime.parse(time);
    LocalDateTime reservationDateTime = LocalDateTime.of(localDate, localTime);

    // Get available tables
    List<Table> availableTables = tableService.getTablesByStatus("AVAILABLE");

    // Calculate total available seats
    int totalAvailableSeats = availableTables.stream()
            .mapToInt(Table::getCapacity)
            .sum();

    Map<String, Object> data = new HashMap<>();
    data.put("availableSeats", totalAvailableSeats);
    data.put("availableTables", availableTables.size());
    data.put("date", date);
    data.put("time", time);
    data.put("tables", availableTables.stream()
            .map(TableResponse::fromEntity)
            .collect(Collectors.toList()));

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy thông tin bàn trống thành công",
        data
    ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "data": {
        "availableSeats": 40,
        "availableTables": 10,
        "date": "2026-05-30",
        "time": "18:00",
        "tables": [
            { "id": 1, "name": "Bàn 1", "capacity": 4, "status": "AVAILABLE", "location": "Tầng 1" }
        ]
    }
}
```

---

### 6.4 POST /client/reservations — Khách hàng đặt bàn

**Controller:** `ClientReservationApiController.createReservation()`
**Service:** `IReservationService` + `ReservationPayloadResolver` + `ReservationRequestValidator`

**HTTP Request:**
```
POST /api/v1/client/reservations
Content-Type: application/json

{
    "name": "Trần Thị D",
    "email": "tranthid@email.com",
    "phone": "0912345678",
    "date": "2026-05-30",
    "time": "18:00",
    "numberOfPeople": 4,
    "orderDetails": "Combo Trưa Văn Phòng x 2",
    "note": "Có trẻ em đi cùng",
    "orderType": "food"
}
```

**Logic cần thực hiện:**

```
1. Resolve user info từ JWT (nếu đã login):
   → payloadResolver.resolveUserInfo(authentication)
   → Nếu có JWT → lấy email từ token → tìm user
   → Nếu không JWT → dùng thông tin từ request body

2. Resolve datetime:
   → payloadResolver.resolveReservationAt(request)
   → Kết hợp date + time thành LocalDateTime

3. Resolve orderDetails:
   → payloadResolver.resolveOrderDetails(request)
   → Gán default nếu empty

4. Validate:
   → validator.validate(request)
   → Kiểm tra các trường bắt buộc
   → Kiểm tra giờ hoạt động (6:00 - 22:00)

5. Tạo orderId duy nhất:
   → orderId = UUID.randomUUID().toString()

6. Check & deduct inventory từ Redis (nếu có):
   → Kiểm tra số lượng món trong kho
   → Nếu đủ → decrement Redis
   → Nếu không đủ → BusinessValidationException

7. Enqueue vào Redis queue:
   → reservationQueue.enqueue(reservationData)

8. Trả về: 201 Created + orderId
```

**Code mẫu:**

```java
@PostMapping
public ResponseEntity<ApiResponse<Map<String, String>>> createReservation(
        @Valid @RequestBody CreateReservationRequest request,
        Authentication auth) {

    // 1. Resolve user info from JWT (if logged in)
    payloadResolver.resolveUserInfo(authentication, request);

    // 2. Resolve datetime
    payloadResolver.resolveReservationAt(request);

    // 3. Resolve order details
    payloadResolver.resolveOrderDetails(request);

    // 4. Validate
    validator.validate(request);

    // 5. Generate orderId
    String orderId = UUID.randomUUID().toString();

    // 6. Create reservation
    boolean success = reservationService.createReservation(
        request.getName(),
        request.getEmail(),
        request.getPhone(),
        request.getDate(),
        request.getTime(),
        request.getNumberOfPeople(),
        request.getOrderDetails(),
        request.getOrderType(),
        orderId
    );

    if (!success) {
        throw new BusinessValidationException("Tạo reservation thất bại. Vui lòng thử lại.");
    }

    Map<String, String> data = new HashMap<>();
    data.put("orderId", orderId);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Đặt bàn thành công! Mã đơn hàng của bạn: " + orderId,
            data
        ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "message": "Đặt bàn thành công! Mã đơn hàng của bạn: abc123-def456",
    "data": {
        "orderId": "abc123-def456"
    },
    "timestamp": "2026-05-30T12:00:00"
}
```

---

## 7. DASHBOARD - Thống kê

### GET /admin/stats/revenue — Thống kê doanh thu

**Controller:** `DashboardApiController.getRevenueStats()`
**Service:** `IReservationService.getTotalRevenue()`

**HTTP Request:**
```
GET /api/v1/admin/stats/revenue
Authorization: Bearer <token>
```

**Logic cần thực hiện:**

```
1. Lấy tổng doanh thu:
   → totalRevenue = reservationService.getTotalRevenue()
   → SELECT COALESCE(SUM(total_price), 0) FROM reservations WHERE status != 'CANCELLED'

2. Lấy doanh thu theo tháng:
   → Tạo map: key = "2026-05", value = tổng tiền tháng đó
   → SELECT SUM(total_price) FROM reservations 
     WHERE status != 'CANCELLED'
     AND YEAR(reservation_at) = ?
     AND MONTH(reservation_at) = ?

3. Build response data:
   data = {
     "totalRevenue": 50000000,          // Tổng doanh thu (VNĐ)
     "monthlyRevenue": {               // Doanh thu theo tháng
       "2026-01": 5000000,
       "2026-02": 7500000,
       "2026-03": 10000000
     }
   }

4. Trả về: 200 OK
```

**Code mẫu:**

```java
@GetMapping("/revenue")
public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueStats() {
    Double totalRevenue = reservationService.getTotalRevenue();

    Map<String, Object> data = new HashMap<>();
    data.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

    // Có thể thêm doanh thu theo tháng nếu cần
    // data.put("monthlyRevenue", monthlyRevenue);

    return ResponseEntity.ok(ApiResponse.success(
        "Lấy thống kê doanh thu thành công",
        data
    ));
}
```

**Đầu ra:**
```json
{
    "success": true,
    "data": {
        "totalRevenue": 50000000
    },
    "timestamp": "2026-05-30T12:00:00"
}
```

---

## 8. Tổng hợp Repository Methods

### UserRepository

```java
// Các method Spring Data tự generate từ method name:
User findByEmail(String email);
Boolean existsByEmail(String email);
List<User> findByRole(String role);
Optional<User> findById(Long id);

// Custom query - tự định nghĩa:
@Query("SELECT u FROM User u WHERE " +
    "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
    "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

// Check email tồn tại (trừ user hiện tại - dùng cho update):
@Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND (:id IS NULL OR u.id != :id)")
Boolean checkEmailExist(@Param("email") String email, @Param("id") Long id);
```

### TableRepository

```java
List<Table> findByStatus(String status);
Page<Table> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
Page<Table> findByNameContainingIgnoreCaseAndStatus(
    String keyword, String status, Pageable pageable);

// Tìm bàn trống (không overlap với reservation):
@Query(value = "SELECT t.* FROM tables t " +
    "LEFT JOIN reservation_tables rt ON t.id = rt.table_id " +
    "LEFT JOIN reservations r ON rt.reservation_id = r.id " +
    "WHERE t.status = 'AVAILABLE' " +
    "AND t.deleted_at IS NULL " +
    "AND (rt.id IS NULL OR r.status = 'CANCELLED' " +
    "     OR r.reservation_at > :endTime " +
    "     OR (r.reservation_at + INTERVAL 2 HOUR) <= :startTime) " +
    "GROUP BY t.id",
    nativeQuery = true)
List<Table> findAvailableTables(
    @Param("startTime") Timestamp startTime,
    @Param("endTime") Timestamp endTime);
```

### FoodRepository

```java
Page<Food> findByStatus(String status, Pageable pageable);
Page<Food> findByMealType(String mealType, Pageable pageable);
Page<Food> findByStatusAndMealType(String status, String mealType, Pageable pageable);

@Query(value = "SELECT f FROM Food f LEFT JOIN FETCH f.createdBy " +
    "WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    "   OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))",
    countQuery = "SELECT COUNT(f) FROM Food f")
Page<Food> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

// Lấy 6 món cho homepage:
@Query("SELECT f FROM Food f WHERE f.status = 'AVAILABLE' ORDER BY f.createdAt DESC")
List<Food> findFirst6Foods();
```

### ComboRepository

```java
Page<Combo> findByStatus(String status, Pageable pageable);
@Query("SELECT c FROM Combo c WHERE c.status = 'available' ORDER BY c.price")
List<Combo> findAvailableCombos();

@Query(value = "SELECT c FROM Combo c " +
    "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    "   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
Page<Combo> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
```

### ReservationRepository

```java
// EntityGraph để eager load:
@EntityGraph(attributePaths = {"customer", "table"})
Optional<Reservation> findByOrderId(String orderId);

// Tìm kiếm theo keyword:
@Query("SELECT r FROM Reservation r " +
    "WHERE LOWER(r.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    "   OR LOWER(r.customer.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    "   OR LOWER(r.note) LIKE LOWER(CONCAT('%', :keyword, '%'))")
Page<Reservation> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

// Tổng doanh thu (không tính CANCELLED):
@Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r " +
    "WHERE r.status != 'CANCELLED'")
Double getTotalRevenue();

// Đếm theo status:
Long countByStatus(String status);
```

---

## Phụ lục: Các Enum values

### UserRole
- `ADMIN` → "admin"
- `STAFF` → "staff"
- `CLIENT` → "client"

### TableStatus
- `AVAILABLE` → "AVAILABLE"
- `OCCUPIED` → "OCCUPIED"
- `RESERVED` → "RESERVED"

### FoodStatus
- `AVAILABLE` → "AVAILABLE"
- `UNAVAILABLE` → "UNAVAILABLE"

### MealType
- `BREAKFAST`
- `LUNCH`
- `DINNER`
- `DESSERT`

### ReservationStatus
- `PENDING` → "PENDING"
- `CONFIRMED` → "CONFIRMED"
- `CANCELLED` → "CANCELLED"
- `COMPLETED` → "COMPLETED"

## Phụ lục: Exception Mapping

| Exception | HTTP Status | Dùng khi |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity không tồn tại (user/table/food/combo/reservation id không đúng) |
| `DuplicateResourceException` | 409 | Email/phone trùng khi tạo user, tên bàn trùng, ... |
| `BusinessValidationException` | 400 | Logic nghiệp vụ: xóa admin, bàn không AVAILABLE, giờ ngoài 6h-22h, ... |
| `MethodArgumentNotValidException` | 400 | Validation annotation fail (@NotBlank, @Email, @Min, ...) |

## Phụ lục: Response Format chuẩn

```json
{
    "success": true,
    "message": "Thông báo thành công",
    "data": { ... },
    "timestamp": "2026-05-30T12:00:00"
}
```

```json
{
    "success": false,
    "message": "Lỗi: Email đã được sử dụng",
    "timestamp": "2026-05-30T12:00:00"
}
```

## Phụ lục: Bảo mật - Phân quyền API

| Endpoint | Yêu cầu | Role |
|----------|----------|------|
| `POST /auth/login` | Public | - |
| `POST /auth/register` | Public | - |
| `POST /auth/verify-otp` | Public | - |
| `GET /client/**` | Public | - |
| `POST /client/reservations` | Public (JWT optional) | - |
| `GET /admin/**` | JWT Required | ROLE_ADMIN |
| `POST /admin/**` | JWT Required | ROLE_ADMIN |
| `PUT /admin/**` | JWT Required | ROLE_ADMIN |
| `DELETE /admin/**` | JWT Required | ROLE_ADMIN |
