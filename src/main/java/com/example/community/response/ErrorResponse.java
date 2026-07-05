package com.example.community.response;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String code;
    private final Object data;

    private ErrorResponse(String code) {
        this.code = code;
        this.data = null;
    }

    public static ErrorResponse of(String code) {
        return new ErrorResponse(code);
    }
}
