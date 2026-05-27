package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.ReservationTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReservationTableRepository extends JpaRepository<ReservationTable, Long> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM reservation_tables WHERE reservation_id = :reservationId", nativeQuery = true)
    void deleteByReservationId(@Param("reservationId") Long reservationId);
}
