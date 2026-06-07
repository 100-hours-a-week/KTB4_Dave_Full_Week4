package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.token.Token;
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
import com.example.community.util.TokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public UserController(@Qualifier("userJsonService") UserService userService,
                          @Qualifier("refreshTokenJsonService") RefreshTokenService refreshTokenService,
                          TokenProvider tokenProvider, ObjectMapper objectMapper){
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@RequestBody SignUpRequest signUpRequest){
        return ResponseEntity.created(URI.create("/users/state")).
                body(ApiResponse.of("회원가입 성공", userService.signUp(signUpRequest)));
    }

    @PostMapping("/state")
    public ResponseEntity<ApiResponse<UserResponse>> signIn(@RequestBody SignInRequest signInRequest){
        UserResponse userResponse = userService.signIn(signInRequest);
        Token accessToken = tokenProvider.createAccessToken(userResponse.userNum(), userResponse.userRole());
        Token refreshToken = tokenProvider.createRefreshToken(userResponse.userNum(), userResponse.userRole());

        refreshTokenService.addRefreshToken(refreshToken);
        String refreshJson = objectMapper.writeValueAsString(refreshToken);
        String accessJson = objectMapper.writeValueAsString(accessToken);

        String refreshEncoded = URLEncoder.encode(refreshJson, StandardCharsets.UTF_8);
        String accessEncoded = URLEncoder.encode(accessJson, StandardCharsets.UTF_8);

        ResponseCookie cookie = ResponseCookie.from("refresh", refreshEncoded)
                .httpOnly(true)
                .secure(true)
                .maxAge(Duration.ofMinutes(30))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header("Authorization", accessEncoded)
                .body(ApiResponse.of("로그인 성공",userResponse));
    }

    @DeleteMapping("/state")
    public ResponseEntity<ApiResponse<Object>> signOut(@RequestHeader("Authorization") String token, @CookieValue(value = "refresh", required = false) String refreshToken){
        String decoded = URLDecoder.decode(refreshToken, StandardCharsets.UTF_8);
        Token refresh = objectMapper.readValue(decoded, Token.class);
        refreshTokenService.deleteRefreshToken(refresh);
        return ResponseEntity.ok(ApiResponse.of("로그아웃 성공", null));
    }

    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateInfo(@RequestHeader("Authorization") String token ,@RequestBody UserInfoRequest userInfoRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);
        return ResponseEntity.ok(ApiResponse.of("회원정보 수정 완료",userService.updateUserInfo(access,userInfoRequest)));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestHeader("Authorization") String token, @RequestBody PasswordChangeRequest passwordChangeRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        userService.changePassword(access, passwordChangeRequest);
        return ResponseEntity.ok(ApiResponse.of("비밀번호 변경 완료", null));
    }

    @DeleteMapping()
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteUser(@RequestHeader("Authorization") String token){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.ok(ApiResponse.of("회원탈퇴 완료", userService.deleteUser(access)));
    }
}
