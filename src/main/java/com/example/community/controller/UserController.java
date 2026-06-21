package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.*;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.service.RefreshTokenService;
import com.example.community.service.user.UserService;
import com.example.community.util.JWTUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JWTUtil jwtUtil;

    public UserController(@Qualifier("userJpaService") UserService userService,
                          @Qualifier("refreshTokenJsonService") RefreshTokenService refreshTokenService,
                          JWTUtil jwtUtil){
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest signUpRequest){
        return ResponseEntity.created(URI.create("/users/state")).
                body(ApiResponse.of("회원가입 성공", userService.signUp(signUpRequest)));
    }

    @PostMapping("/state")
    public ResponseEntity<ApiResponse<SignInResponse>> signIn(@RequestBody @Valid SignInRequest signInRequest){
        UserInfoDTO userInfoDTO = userService.signIn(signInRequest);
        String accessToken = jwtUtil.generateAccessToken(userInfoDTO.userNum(), userInfoDTO.profileId(), userInfoDTO.userRole());
        String refreshToken = jwtUtil.generateRefreshToken(userInfoDTO.userNum());
        SignInResponse signInResponse = SignInResponse.of(userInfoDTO, accessToken);

        refreshTokenService.addRefreshToken(userInfoDTO.userNum(), refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ofMinutes(30))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.of("로그인 성공",signInResponse));
    }

    @DeleteMapping("/state")
    public ResponseEntity<ApiResponse<Object>> signOut(@SignUser SignUserInfo signUserInfo, @CookieValue(value = "refresh") String refreshToken){
        refreshTokenService.deleteRefreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.of("로그아웃 성공", null));
    }

    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateInfo(@SignUser SignUserInfo signUserInfo, @RequestBody @Valid UserInfoRequest userInfoRequest){
        return ResponseEntity.ok(ApiResponse.of("회원정보 수정 완료",userService.updateUserInfo(signUserInfo,userInfoRequest)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@SignUser SignUserInfo signUserInfo, @RequestBody @Valid PasswordChangeRequest passwordChangeRequest){
        userService.changePassword(signUserInfo, passwordChangeRequest);
        return ResponseEntity.ok(ApiResponse.of("비밀번호 변경 완료", null));
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteUser(@SignUser SignUserInfo signUserInfo){
        return ResponseEntity.ok(ApiResponse.of("회원탈퇴 완료", userService.deleteUser(signUserInfo)));
    }
}
