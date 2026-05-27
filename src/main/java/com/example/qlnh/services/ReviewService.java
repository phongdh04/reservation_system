package com.example.qlnh.services;

import com.example.qlnh.models.entities.Review;
import com.example.qlnh.models.entities.User;
import com.example.qlnh.repositories.ReviewRepository;
import com.example.qlnh.repositories.UserRepository;
import com.example.qlnh.services.interfaces.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // TODO: VIET LOGIC - tao review, tao user neu chua co
    @Override
    @Transactional
    public boolean createReview(String name, String email, String phone, String content) {
        throw new UnsupportedOperationException("TODO: Implement createReview logic");
    }
}
