package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.ReservationFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReservationFoodRepository extends JpaRepository<ReservationFood, Long> {
    void deleteByFoodId(Long foodId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM reservation_foods WHERE reservation_id = :reservationId", nativeQuery = true)
    void deleteByReservationId(@Param("reservationId") Long reservationId);
}
