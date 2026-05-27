package com.example.qlnh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class QlnhApplication {

    public static void main(String[] args) {
        SpringApplication.run(QlnhApplication.class, args);
    }
}
