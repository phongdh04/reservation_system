package com.example.qlnh.services.interfaces;

public interface IReviewService {
    boolean createReview(String name, String email, String phone, String content);
}
