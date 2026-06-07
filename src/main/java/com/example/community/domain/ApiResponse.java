package com.example.community.domain;


public record ApiResponse<T> (
        String code,
        T data
        ){
    public static <T> ApiResponse<T> of(String code, T data) {
        return new ApiResponse<>(code, data);
    }
}