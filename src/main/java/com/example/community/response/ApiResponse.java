package com.example.community.response;


public record ApiResponse<T> (
        String code,
        T data
        ){
    public static <T> ApiResponse<T> of(String code, T data) {
        return new ApiResponse<>(code, data);
    }
}