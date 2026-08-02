package com.example.community.configuration;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class JwtAuthenticationException extends AuthenticationException {
    private final String code;

    public JwtAuthenticationException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }
}
