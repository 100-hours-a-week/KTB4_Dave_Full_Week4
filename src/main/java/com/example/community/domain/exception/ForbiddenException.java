package com.example.community.domain.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException{
    public ForbiddenException(String code, HttpStatus status) {
        super(code, status);
    }
}
