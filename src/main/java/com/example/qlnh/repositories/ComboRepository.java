package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Combo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    Page<Combo> findByStatus(String status, Pageable pageable);

    @Query("SELECT c FROM Combo c WHERE c.name LIKE %:keyword%")
    Page<Combo> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Combo c WHERE c.name LIKE %:keyword% AND c.status = :status")
    Page<Combo> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query("SELECT c FROM Combo c WHERE c.status = 'available' ORDER BY c.price ASC")
    Page<Combo> findAvailableCombos(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Combo c WHERE c.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT c FROM Combo c WHERE c.name IN :names AND c.status = 'available'")
    List<Combo> findByNameInAndStatusAvailable(@Param("names") List<String> names);
}
