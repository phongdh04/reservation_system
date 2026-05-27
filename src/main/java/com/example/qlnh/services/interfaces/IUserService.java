package com.example.qlnh.services.interfaces;

import com.example.qlnh.models.entities.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IUserService {

    List<User> getAllUsers();
    List<User> getAllUsersSortedByName();
    Page<User> getUsers(int page, int itemsPerPage);
    Page<User> getUsersByRole(int page, int itemsPerPage, String role);
    long getTotalUsers();
    long getTotalUsersByRole(String role);
    User getUserById(Long id);
    User getUserByEmail(String email);
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(Long id);
    boolean checkEmailExist(User user);
    boolean checkPhoneExist(User user);
    boolean comparePassword(String password, String confirmPassword);
    Page<User> findByKeyword(String keyword, int page, int itemsPerPage);
    long getTotalUsersByKeyword(String keyword);
    Page<User> findByKeywordWithRole(String keyword, String role, int page, int itemsPerPage);
    long getTotalUsersByKeywordWithRole(String keyword, String role);
    User registerClient(String name, String email, String phone, String password);
    User verifyOtp(String email, String otp);
    User verifyEmail(String token);
    User findByVerificationToken(String token);
}
