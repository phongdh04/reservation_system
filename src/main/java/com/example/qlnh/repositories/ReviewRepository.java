package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByCustomerId(int customerId, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM reviews WHERE customer_id = :customerId", nativeQuery = true)
    void deleteByCustomerId(@Param("customerId") Long customerId);
}
