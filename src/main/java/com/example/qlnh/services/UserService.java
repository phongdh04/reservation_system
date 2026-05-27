package com.example.qlnh.services;

import com.example.qlnh.exception.ResourceNotFoundException;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.repositories.FoodRepository;
import com.example.qlnh.repositories.ReservationRepository;
import com.example.qlnh.repositories.ReviewRepository;
import com.example.qlnh.repositories.UserRepository;
import com.example.qlnh.services.interfaces.IEmailService;
import com.example.qlnh.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final FoodRepository foodRepository;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsersSortedByName() {
        return userRepository.findAll(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("email")));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(int page, int itemsPerPage) {
        return userRepository.findAll(PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(int page, int itemsPerPage, String role) {
        return userRepository.findByRole(role, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersByRole(String role) {
        return userRepository.countByRole(role);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // TODO: VIET LOGIC
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // TODO: VIET LOGIC
    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        return userRepository.save(existingUser);
    }

    // TODO: VIET LOGIC - xoa user + xoa review/reservation/food lien quan
    @Override
    @Transactional
    public void deleteUser(Long id) {
        throw new UnsupportedOperationException("TODO: Implement deleteUser logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailExist(User user) {
        return userRepository.checkEmailExist(user.getEmail(), user.getId() != null ? user.getId() : 0L) > 0;
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public boolean checkPhoneExist(User user) {
        return userRepository.checkPhoneExist(user.getPhone(), user.getId() != null ? user.getId() : 0L) > 0;
    }

    // TODO: VIET LOGIC
    @Override
    public boolean comparePassword(String password, String confirmPassword) {
        return password.equals(confirmPassword);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<User> findByKeyword(String keyword, int page, int itemsPerPage) {
        return userRepository.findByNameEmailPhone(keyword, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersByKeyword(String keyword) {
        return userRepository.countByNameContainingOrEmailContainingOrPhoneContaining(keyword, keyword, keyword);
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public Page<User> findByKeywordWithRole(String keyword, String role, int page, int itemsPerPage) {
        return userRepository.findByNameEmailPhoneWithRole(keyword, role, PageRequest.of(page - 1, itemsPerPage));
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public long getTotalUsersByKeywordWithRole(String keyword, String role) {
        return userRepository.countByNameContainingOrEmailContainingOrPhoneContainingAndRole(keyword, keyword, keyword, role);
    }

    // TODO: VIET LOGIC - dang ky khach hang moi, tao OTP, gui email
    @Override
    @Transactional
    public User registerClient(String name, String email, String phone, String password) {
        throw new UnsupportedOperationException("TODO: Implement registerClient logic");
    }

    // TODO: VIET LOGIC - xac thuc OTP
    @Override
    @Transactional
    public User verifyOtp(String email, String otp) {
        throw new UnsupportedOperationException("TODO: Implement verifyOtp logic");
    }

    // TODO: VIET LOGIC - xac thuc email bang token
    @Override
    @Transactional
    public User verifyEmail(String token) {
        throw new UnsupportedOperationException("TODO: Implement verifyEmail logic");
    }

    // TODO: VIET LOGIC
    @Override
    @Transactional(readOnly = true)
    public User findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token);
    }
}
