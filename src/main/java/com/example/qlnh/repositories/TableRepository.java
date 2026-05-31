package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Table; // Hoặc RestaurantTable nếu bạn đã đổi tên
import com.example.qlnh.models.enums.TableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface TableRepository extends JpaRepository<Table, Long> {

       // 1. Phục vụ Validate lúc Create/Update
       boolean existsByName(String name);

       // 2. Siêu hàm Gộp: Thay thế toàn bộ các hàm findByStatus, findByKeyword...
       @Query("SELECT t FROM Table t WHERE " +
                     "(:keyword IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "   OR LOWER(t.location) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "AND (:status IS NULL OR t.status = :status)")
       Page<Table> searchTables(@Param("keyword") String keyword,
                     @Param("status") TableStatus status,
                     Pageable pageable);

       // ==============================================================
       // NHÓM TRUY VẤN NGHIỆP VỤ ĐẶT BÀN (RESERVATION)
       // ==============================================================

       // Đã xóa "t.deletedAt IS NULL" vì Hibernate tự động lo việc đó
       // Lưu ý: Đổi 'available' thành 'AVAILABLE' cho khớp Enum
       @Query("SELECT COALESCE(SUM(t.capacity), 0) FROM Table t " +
                     "WHERE t.status = 'AVAILABLE' " +
                     "AND NOT EXISTS (SELECT 1 FROM Reservation r WHERE r.table.id = t.id AND r.status <> 'CANCELLED' "
                     +
                     "AND r.reservationAt < :endTime AND r.reservationAt > :overlapStart)")
       int sumAvailableCapacity(@Param("endTime") Timestamp endTime, @Param("overlapStart") Timestamp overlapStart);

       @Query("SELECT COALESCE(SUM(t.capacity), 0) FROM Table t WHERE t.status = 'AVAILABLE'")
       int sumAllAvailableCapacity();

       // Đã đổi List<Integer> thành List<Long> vì ID của Table là kiểu Long
       @Query("SELECT t.id FROM Table t WHERE t.status = 'AVAILABLE' " +
                     "AND t.capacity >= :numberOfPeople AND NOT EXISTS (SELECT 1 FROM Reservation r " +
                     "WHERE r.table.id = t.id AND r.status <> 'CANCELLED' " +
                     "AND r.reservationAt < :endTime AND r.reservationAt > :overlapStart) ORDER BY t.capacity ASC")
       List<Long> findAvailableTableIds(@Param("numberOfPeople") int numberOfPeople,
                     @Param("endTime") Timestamp endTime,
                     @Param("overlapStart") Timestamp overlapStart);
}