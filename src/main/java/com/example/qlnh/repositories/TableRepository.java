package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Table;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface TableRepository extends JpaRepository<Table, Long> {

    Page<Table> findByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM Table t WHERE t.name LIKE %:keyword% OR t.location LIKE %:keyword%")
    Page<Table> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT t FROM Table t WHERE (t.name LIKE %:keyword% OR t.location LIKE %:keyword%) AND t.status = :status")
    Page<Table> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Table t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(t.capacity), 0) FROM Table t " +
           "WHERE t.status = 'available' AND t.deletedAt IS NULL " +
           "AND NOT EXISTS (SELECT 1 FROM Reservation r WHERE r.table.id = t.id AND r.status <> 'cancelled' " +
           "AND r.reservationAt < :endTime AND r.reservationAt > :overlapStart)")
    int sumAvailableCapacity(@Param("endTime") Timestamp endTime, @Param("overlapStart") Timestamp overlapStart);

    @Query("SELECT COALESCE(SUM(t.capacity), 0) FROM Table t WHERE t.status = 'available' AND t.deletedAt IS NULL")
    int sumAllAvailableCapacity();

    @Query("SELECT t.id FROM Table t WHERE t.status = 'available' AND t.deletedAt IS NULL " +
           "AND t.capacity >= :numberOfPeople AND NOT EXISTS (SELECT 1 FROM Reservation r " +
           "WHERE r.table.id = t.id AND r.status <> 'cancelled' " +
           "AND r.reservationAt < :endTime AND r.reservationAt > :overlapStart) ORDER BY t.capacity ASC")
    List<Integer> findAvailableTableIds(@Param("numberOfPeople") int numberOfPeople, @Param("endTime") Timestamp endTime, @Param("overlapStart") Timestamp overlapStart);
}
