package com.example.community.domain.exception;

import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends BusinessException{
    public UnAuthorizedException(String code, HttpStatus status) {
        super(code, status);
    }
}
