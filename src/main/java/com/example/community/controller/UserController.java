package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.SignUpResponse;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.domain.user.response.UserResponse;
import com.example.community.service.RefreshTokenService;
import com.example.community.service.UserService;
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

    public UserController(@Qualifier("userJsonService") UserService userService,
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
    public ResponseEntity<ApiResponse<UserResponse>> signIn(@RequestBody @Valid SignInRequest signInRequest){
        UserResponse userResponse = userService.signIn(signInRequest);
        String accessToken = jwtUtil.generateAccessToken(userResponse.userNum(), userResponse.userRole());
        String refreshToken = jwtUtil.generateRefreshToken(userResponse.userNum());

        refreshTokenService.addRefreshToken(userResponse.userNum(), refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(false)
                .maxAge(Duration.ofMinutes(30))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header("Authorization", accessToken)
                .body(ApiResponse.of("로그인 성공",userResponse));
    }

    @DeleteMapping("/state")
    public ResponseEntity<ApiResponse<Object>> signOut(@RequestHeader("Authorization") String token, @CookieValue(value = "refresh", required = false) String refreshToken){

        refreshTokenService.deleteRefreshToken("");
        return ResponseEntity.ok(ApiResponse.of("로그아웃 성공", null));
    }

    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateInfo(@RequestHeader("Authorization") String token ,@RequestBody @Valid UserInfoRequest userInfoRequest){

        //userNum 체크
        return ResponseEntity.ok(ApiResponse.of("회원정보 수정 완료",userService.updateUserInfo(token,userInfoRequest)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestHeader("Authorization") String token, @RequestBody @Valid PasswordChangeRequest passwordChangeRequest){


        userService.changePassword(token, passwordChangeRequest);
        return ResponseEntity.ok(ApiResponse.of("비밀번호 변경 완료", null));
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteUser(@RequestHeader("Authorization") String token){

        return ResponseEntity.ok(ApiResponse.of("회원탈퇴 완료", userService.deleteUser(token)));
    }
}
