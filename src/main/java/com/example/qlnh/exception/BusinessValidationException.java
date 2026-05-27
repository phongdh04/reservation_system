package com.example.qlnh.exception;

import org.springframework.http.HttpStatus;

public class BusinessValidationException extends BaseException {
    public BusinessValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessValidationException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}
