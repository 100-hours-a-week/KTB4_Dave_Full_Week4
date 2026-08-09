package com.example.community.user.controller;

import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.service.AuthService;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.response.SignInResponse;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.service.UserAccountCommandService;
import com.example.community.user.service.UserAvailabilityQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserAccountController {
    private final UserAccountCommandService accountCommandService;
    private final UserAvailabilityQueryService availabilityQueryService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(
            @ModelAttribute @Valid SignUpRequest request
    ) {
        return ResponseEntity.created(URI.create("/users/state"))
                .body(ApiResponse.of(
                        "회원가입 성공",
                        accountCommandService.signUp(request)
                ));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Object>> checkEmailDuplicate(
            @RequestBody String email
    ) {
        if (availabilityQueryService.isExistEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.of("중복 이메일 존재", null));
        }
        return ResponseEntity.ok(ApiResponse.of(
                "가입 가능한 이메일",
                null
        ));
    }

    @PostMapping("/nickname")
    public ResponseEntity<ApiResponse<Object>> checkNicknameDuplicate(
            @RequestBody String nickname
    ) {
        if (availabilityQueryService.isExistNickname(nickname)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.of("중복 닉네임 존재", null));
        }
        return ResponseEntity.ok(ApiResponse.of(
                "사용 가능한 닉네임",
                null
        ));
    }

    @PostMapping("/state")
    public ResponseEntity<ApiResponse<SignInResponse>> signIn(
            @RequestBody @Valid SignInRequest request
    ) {
        AuthResponse authResponse = authService.signIn(request);
        ResponseCookie cookie = refreshCookie(authResponse.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.of(
                        "로그인 성공",
                        authResponse.signInResponse()
                ));
    }

    @DeleteMapping("/state")
    public ResponseEntity<ApiResponse<Object>> signOut(
            @CookieValue(value = "refresh", required = false)
            String refreshToken
    ) {
        authService.signOut(refreshToken);
        return ResponseEntity.ok(ApiResponse.of(
                "로그아웃 성공",
                null
        ));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @SignUser SignUserInfo signUserInfo,
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        accountCommandService.changePassword(signUserInfo, request);
        return ResponseEntity.ok(ApiResponse.of(
                "비밀번호 변경 완료",
                null
        ));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteUser(
            @SignUser SignUserInfo signUserInfo
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "회원탈퇴 완료",
                accountCommandService.deleteUser(signUserInfo)
        ));
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMinutes(30))
                .sameSite("Lax")
                .build();
    }
}
