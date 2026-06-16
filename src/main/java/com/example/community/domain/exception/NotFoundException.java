package com.example.community.domain.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException{
    public NotFoundException(String code, HttpStatus status) {
        super(code, status);
    }
}
