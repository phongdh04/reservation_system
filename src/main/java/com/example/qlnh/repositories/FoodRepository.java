package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    @EntityGraph(attributePaths = { "createdBy" })
    Page<Food> findByMealType(String mealType, Pageable pageable);

    @EntityGraph(attributePaths = { "createdBy" })
    Page<Food> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = { "createdBy" })
    Page<Food> findByStatusAndMealType(String status, String mealType, Pageable pageable);

    @Query(value = "SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.name LIKE %:keyword%", countQuery = "SELECT COUNT(f) FROM Food f WHERE f.name LIKE %:keyword%")
    Page<Food> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.name LIKE %:keyword% AND f.status = :status", countQuery = "SELECT COUNT(f) FROM Food f WHERE f.name LIKE %:keyword% AND f.status = :status")
    Page<Food> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.status = 'available' ORDER BY f.id ASC", countQuery = "SELECT COUNT(f) FROM Food f WHERE f.status = 'available'")
    Page<Food> findFirst6Foods(Pageable pageable);

    @Query(value = "SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.status = 'available' AND f.mealType = :mealType ORDER BY f.id ASC", countQuery = "SELECT COUNT(f) FROM Food f WHERE f.status = 'available' AND f.mealType = :mealType")
    Page<Food> find6FoodsByMealType(@Param("mealType") String mealType, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Food f WHERE f.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.createdBy.id = :userId")
    List<Food> findByCreatedBy(@Param("userId") Long userId);

    @Query("SELECT f FROM Food f WHERE f.name IN :names AND f.status = 'available'")
    List<Food> findByNameInAndStatusAvailable(@Param("names") List<String> names);

    @Query("SELECT f FROM Food f JOIN FETCH f.createdBy WHERE f.status = 'available' AND f.mealType = :mealType ORDER BY f.name ASC")
    List<Food> findAllByMealTypeOrderByNameAsc(@Param("mealType") String mealType);
}
