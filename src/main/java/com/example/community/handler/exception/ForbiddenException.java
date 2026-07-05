package com.example.community.handler.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException{
    public ForbiddenException(String code) {
        super(code, HttpStatus.FORBIDDEN);
    }
}
