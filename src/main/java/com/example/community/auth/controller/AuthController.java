package com.example.community.auth.controller;

import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class authController {
    @PostMapping
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@CookieValue(value = "refresh") String refreshToken){
        return ResponseEntity.ok(ApiResponse.of("리프레시 성공", null));
    }
}
