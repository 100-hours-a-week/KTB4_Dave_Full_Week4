package com.example.community.user.controller;

import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.service.UserProfileCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserProfileController {
    private final UserProfileCommandService profileCommandService;

    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateInfo(
            @SignUser SignUserInfo signUserInfo,
            @ModelAttribute @Valid UserInfoRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.of(
                "회원정보 수정 완료",
                profileCommandService.updateUserInfo(signUserInfo, request)
        ));
    }
}
