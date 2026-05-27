package com.example.qlnh;

import com.example.qlnh.models.entities.*;
import com.example.qlnh.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader {

    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final FoodRepository foodRepository;
    private final ComboRepository comboRepository;
    private final ComboFoodRepository comboFoodRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void loadData() {
        if (userRepository.count() == 0) {
            log.info("Loading initial data...");

            // TODO: Tao admin mac dinh
            // admin@gmail.com / 12345678

            // TODO: Tao staff mac dinh

            // TODO: Tao cac ban mac dinh (A1, A2, B1, B2, C1, C2, VIP1)

            // TODO: Tao cac mon an mac dinh (10 mon)

            // TODO: Tao cac combo mac dinh (3 combo)

            // TODO: Tao combo_foods

            log.info("Initial data loaded successfully.");
        }
    }
}
