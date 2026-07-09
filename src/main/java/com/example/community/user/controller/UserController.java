package com.example.community.user.controller;

import com.example.community.refreshToken.service.RefreshTokenService;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignInResponse;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.service.UserService;
import com.example.community.util.JWTUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JWTUtil jwtUtil;

    @PostMapping()
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@ModelAttribute @Valid SignUpRequest signUpRequest) throws IOException {
        return ResponseEntity.created(URI.create("/users/state")).
                body(ApiResponse.of("회원가입 성공", userService.signUp(signUpRequest)));
    }

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Object>> checkEmailDuplicate(@RequestBody String email){
        return userService.isExistEmail(email)?
                ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.of("중복 이메일 존재", null)) :
                ResponseEntity.ok(ApiResponse.of("가입 가능한 이메일", null));
    }

    @PostMapping("/nickname")
    public ResponseEntity<ApiResponse<Object>> checkNicknameDuplicate(@RequestBody String nickname){
        return userService.isExistNickname(nickname)?
                ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.of("중복 닉네임 존재", null)) :
                ResponseEntity.ok(ApiResponse.of("사용 가능한 닉네임", null));
    }


    @PostMapping("/state")
    public ResponseEntity<ApiResponse<SignInResponse>> signIn(@RequestBody @Valid SignInRequest signInRequest){
        UserInfoDTO userInfoDTO = userService.signIn(signInRequest);
        String accessToken = jwtUtil.generateAccessToken(userInfoDTO.getUserNum(), userInfoDTO.getProfileId(), userInfoDTO.getUserRole());
        String refreshToken = jwtUtil.generateRefreshToken(userInfoDTO.getUserNum());
        SignInResponse signInResponse = SignInResponse.of(userInfoDTO, accessToken);

        refreshTokenService.addRefreshToken(userInfoDTO.getUserNum(), refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofMinutes(30))
                .sameSite("Lax")
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
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateInfo(@SignUser SignUserInfo signUserInfo, @ModelAttribute @Valid UserInfoRequest userInfoRequest) throws IOException {
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
