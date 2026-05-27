# qlnh - Restaurant Reservation Management System

Base project da duoc thiet ke san de ban tu code theo huong.

## Du an nay gom

### Backend Structure

```
src/main/java/com/example/qlnh/
  models/
    entities/       - 12 Entity classes (User, Table, Food, Combo, Reservation...)
    enums/           - 5 Enum classes (UserRole, FoodStatus, ReservationStatus...)
  repositories/      - 10 Repository interfaces (JPA)
  services/
    interfaces/      - 7 Service interfaces
    *.java           - 8 Service implementations (LOGIC TRONG)
  controllers/api/   - 12 Controller classes (LOGIC TRONG)
  dto/
    request/        - 8 Request DTO classes
    response/       - 6 Response DTO classes
  exception/         - 5 Exception classes + GlobalExceptionHandler
  config/            - SecurityConfig, RedisConfig
  filter/            - JwtAuthFilter
  helpers/            - JwtTokenProvider, CurrentUserResolver, ReservationPayloadResolver, ReservationRequestValidator
  aspect/            - LoggingAspect
  DataLoader.java    - Load du lieu ban dau (LOGIC TRONG)

src/main/resources/
  application.properties
database.sql         - Schema cua database
```

## Cac buoc de chay

### 1. Tao Database
```sql
CREATE DATABASE IF NOT EXISTS qlnh_db;
USE qlnh_db;
-- Chay file database.sql
```

### 2. Cau hinh application.properties
Chinh sua cac thong so trong `application.properties`:
- Database: host, port, username, password
- Redis: host, port, password
- Email: username, password (App Password Gmail)
- JWT: secret, expiration

### 3. Build & Run
```bash
./mvnw spring-boot:run
```

## Danh sach TODO - Viec can lam

### Service Layer (10 files)
| File | Method | Mo ta |
|------|--------|-------|
| **UserService** | `registerClient` | Tao user moi, ma hoa password, tao OTP, gui email |
| **UserService** | `verifyOtp` | Xac thuc OTP 6 so |
| **UserService** | `verifyEmail` | Xac thuc email bang token |
| **UserService** | `deleteUser` | Xoa user + review/reservation/food lien quan |
| **TableService** | `deleteTable` | Soft delete (set deletedAt) |
| **FoodService** | `deleteFood` | Xoa food + khoi combo_foods/reservation_foods |
| **ComboService** | `createComboWithFoods` | Tao combo + tao combo_foods |
| **ComboService** | `updateComboWithFoods` | Update combo + xoa/tao lai combo_foods |
| **ComboService** | `deleteCombo` | Xoa combo (kiem tra reservation_combos) |
| **ReservationService** | `createReservation` | Tao reservation, kiem tra gio, tim ban, tinh tong tien |
| **ReservationService** | `assignTable` | Gan ban cho reservation |
| **ReservationService** | `deleteReservationById` | Huy reservation |
| **ReviewService** | `createReview` | Tao review, tao user neu chua co |
| **EmailService** | `sendVerificationEmail` | Gui email xac nhan voi link |
| **EmailService** | `sendOtpEmail` | Gui email OTP |

### Controller Layer (12 files)
| Controller | Endpoint | Mo ta |
|-----------|---------|-------|
| **AuthApiController** | POST `/api/v1/auth/login` | Dang nhap, tra ve JWT |
| **AuthApiController** | POST `/api/v1/auth/register` | Dang ky khach hang |
| **AuthApiController** | POST `/api/v1/auth/verify-otp` | Xac thuc OTP |
| **AuthApiController** | GET `/api/v1/auth/verify-email` | Xac thuc email |
| **AuthApiController** | GET `/api/v1/auth/me` | Lay thong tin user hien tai |
| **UserApiController** | CRUD `/api/v1/admin/users` | Quan ly nguoi dung |
| **TableApiController** | CRUD `/api/v1/admin/tables` | Quan ly ban |
| **FoodApiController** | CRUD `/api/v1/admin/foods` | Quan ly mon an |
| **ComboApiController** | CRUD `/api/v1/admin/combos` | Quan ly combo |
| **ReservationApiController** | CRUD `/api/v1/admin/reservations` | Quan ly dat ban |
| **ClientFoodApiController** | GET `/api/v1/client/foods` | Danh sach mon an cho khach |
| **ClientComboApiController** | GET `/api/v1/client/combos` | Danh sach combo cho khach |
| **ClientTableApiController** | GET `/api/v1/client/tables/availability` | Kiem tra cho trong |
| **ClientReservationApiController** | POST `/api/v1/client/reservations` | Dat ban |
| **SseController** | GET `/api/v1/sse/admin` | SSE real-time |
| **DashboardApiController** | GET `/api/v1/admin/stats/revenue` | Thong ke doanh thu |

### DataLoader
| Noi dung | Mo ta |
|---------|-------|
| Tao admin | admin@gmail.com / 12345678 |
| Tao staff | staff@gmail.com |
| Tao 7 ban | A1, A2, B1, B2, C1, C2, VIP1 |
| Tao 10 mon an | Breakfast, lunch, dinner, dessert |
| Tao 3 combo | Cap doi, gia dinh, tiec nho |

## Database Schema

- **users** - Nguoi dung (admin, staff, client)
- **tables** - Ban ( voi soft delete )
- **foods** - Mon an ( co meal_type, status )
- **combos** - Combo
- **combo_foods** - Chi tiet combo (combo chua nhieu mon)
- **reservations** - Dat ban
- **reservation_foods** - Mon an trong reservation
- **reservation_combos** - Combo trong reservation
- **reservation_tables** - Ban trong reservation
- **reviews** - Danh gia

## Quy tac code

1. Tat ca service deu co interface (di先interface roi implementation)
2. Dung `@Transactional` cho method doc/lghi
3. Dung `@Transactional(readOnly = true)` cho method chi doc
4. Dung `Page` cho danh sach co phan trang
5. Tra ve `ApiResponse<T>` cho tat ca API
6. Dung exception co san (`ResourceNotFoundException`, `BusinessValidationException`, `DuplicateResourceException`)
7. JWT token cho authentication
8. BCrypt cho password hashing
