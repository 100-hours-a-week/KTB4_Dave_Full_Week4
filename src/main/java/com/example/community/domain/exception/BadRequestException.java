package com.example.community.domain.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BusinessException{
    public BadRequestException(String code, HttpStatus status) {
        super(code, status);
    }
}
