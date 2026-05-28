package com.example.qlnh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Lúc bình thường chạy 5 luồng
        executor.setMaxPoolSize(10); // Lúc cao điểm tối đa 10 luồng
        executor.setQueueCapacity(500); // Nếu quá 10 luồng, cho 500 email vào hàng đợi
        executor.setThreadNamePrefix("EmailSender-");
        executor.initialize();
        return executor;
    }
}