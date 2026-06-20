package com.example.community.domain.exception;

import org.springframework.http.HttpStatus;

public class DuplicateException extends BusinessException{

    public DuplicateException(String code) {
        super(code, HttpStatus.CONFLICT);
    }
}
