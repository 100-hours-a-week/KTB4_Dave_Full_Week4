package com.example.community.auth.controller;

import com.example.community.auth.dto.AccessTokenDTO;
import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.auth.service.AuthService;
import com.example.community.response.ApiResponse;
import com.example.community.user.dto.response.SignInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<AccessTokenDTO>> refresh(@CookieValue(value = "refresh") String refreshToken){
        RefreshResponse authResponse = authService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh", authResponse.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMinutes(30))
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.of("리프레시 성공",new AccessTokenDTO(authResponse.accessToken())));
    }
}
