package com.example.community.handler;

import com.example.community.domain.ErrorResponse;
import com.example.community.domain.exception.BusinessException;
import com.example.community.domain.exception.DuplicateException;
import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ErrorResponse handleBusiness(BusinessException exception){
        return ErrorResponse.of(exception.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleNotValid(MethodArgumentNotValidException exception){
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFound(NotFoundException exception){
        return ErrorResponse.of(exception.getCode());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ErrorResponse handleForbidden(ForbiddenException exception){
        return ErrorResponse.of(exception.getCode());
    }

    @ExceptionHandler(DuplicateException.class)
    public ErrorResponse handleDuplicate(DuplicateException exception){
        return ErrorResponse.of(exception.getCode());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception){
        return ErrorResponse.of(exception.getMessage());
    }
}
