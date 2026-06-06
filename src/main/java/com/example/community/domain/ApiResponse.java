package com.example.community.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiResponse<T> {

    private final String code;
    private final T data;

    public static <T> ApiResponse<T> of(String code, T data) {
        return new ApiResponse<>(code, data);
    }
}