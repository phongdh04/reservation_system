package com.example.qlnh.repositories;

import com.example.qlnh.models.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    User findByName(String name);
    User findByEmailOrPhone(String email, String phone);

    @Query("SELECT COUNT(u) FROM User u WHERE u.email = :email AND u.id != :id")
    Long checkEmailExist(@Param("email") String email, @Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.phone = :phone AND u.id != :id")
    Long checkPhoneExist(@Param("phone") String phone, @Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phone LIKE %:keyword%")
    Page<User> findByNameEmailPhone(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE (u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.phone LIKE %:keyword%) AND u.role = :role")
    Page<User> findByNameEmailPhoneWithRole(@Param("keyword") String keyword, @Param("role") String role, Pageable pageable);

    Page<User> findByRole(String role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    User findByVerificationToken(String verificationToken);

    List<User> findAll(Sort sort);

    @Query("SELECT r.id FROM Reservation r WHERE r.customer.id = :customerId")
    List<Long> findReservationIdsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT f.id FROM Food f WHERE f.createdBy.id = :userId")
    List<Long> findFoodIdsByCreatedBy(@Param("userId") Long userId);
}
