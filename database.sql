-- =============================================
-- DATABASE: qlnh_db
-- Run: CREATE DATABASE IF NOT EXISTS qlnh_db;
-- Then: source database.sql;
-- =============================================

-- ── USERS ────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(10),
    otp_expiry TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_phone (phone),
    INDEX idx_users_verification_token (verification_token)
);

-- ── TABLES ───────────────────────────────────
CREATE TABLE IF NOT EXISTS `tables` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    location VARCHAR(255),
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_tables_status (status),
    INDEX idx_tables_deleted_at (deleted_at)
);

-- ── FOODS ────────────────────────────────────
CREATE TABLE IF NOT EXISTS foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price FLOAT NOT NULL,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    meal_type VARCHAR(20),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_foods_status_mealtype (status, meal_type),
    INDEX idx_foods_name (name)
);

-- ── COMBOS ───────────────────────────────────
CREATE TABLE IF NOT EXISTS combos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price FLOAT NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    image_url VARCHAR(500)
);

-- ── COMBO_FOODS ──────────────────────────────
CREATE TABLE IF NOT EXISTS combo_foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    FOREIGN KEY (food_id) REFERENCES foods(id)
);

-- ── RESERVATIONS ─────────────────────────────
CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_people INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reservation_at TIMESTAMP NOT NULL,
    note TEXT,
    order_id VARCHAR(36) UNIQUE,
    total_price FLOAT,
    customer_id BIGINT NOT NULL,
    table_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (table_id) REFERENCES `tables`(id),
    INDEX idx_res_status_reserved_at (status, reservation_at),
    INDEX idx_res_customer_id (customer_id),
    INDEX idx_res_table_id (table_id),
    INDEX idx_res_created_at (created_at DESC)
);

-- ── RESERVATION_FOODS ────────────────────────
CREATE TABLE IF NOT EXISTS reservation_foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    FOREIGN KEY (food_id) REFERENCES foods(id),
    INDEX idx_res_foods_reservation_id (reservation_id),
    INDEX idx_res_foods_food_id (food_id)
);

-- ── RESERVATION_COMBOS ───────────────────────
CREATE TABLE IF NOT EXISTS reservation_combos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    combo_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    INDEX idx_res_combos_reservation_id (reservation_id)
);

-- ── RESERVATION_TABLES ───────────────────────
CREATE TABLE IF NOT EXISTS reservation_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    FOREIGN KEY (table_id) REFERENCES `tables`(id)
);

-- ── REVIEWS ─────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    INDEX idx_reviews_customer_id (customer_id),
    INDEX idx_reviews_created_at (created_at DESC)
);
