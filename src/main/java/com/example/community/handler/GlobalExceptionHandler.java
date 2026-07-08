package com.example.community.handler;

import com.example.community.handler.exception.BusinessException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception){
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(exception.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException exception){
        return ResponseEntity.status(400).body(ErrorResponse.of("입력데이터가 유효하지 않습니다." + exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception){
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(exception.getCode()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception){
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(exception.getCode()));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateException exception){
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(exception.getCode()));

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception){
        return ResponseEntity.status(400).body(ErrorResponse.of(exception.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity
                .status(409)
                .body(ErrorResponse.of(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        exception.printStackTrace(); // 개발 중에는 실제 원인 확인용

        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of("서버 내부 오류가 발생했습니다."));
    }
}
