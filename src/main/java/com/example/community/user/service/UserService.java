package com.example.community.user.service;

import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;

import java.io.IOException;

public interface UserService {
    SignUpResponse signUp(SignUpRequest signUpRequest) throws IOException;
    UserInfoDTO signIn(SignInRequest signInRequest);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoResponse updateUserInfo(SignUserInfo signUserInfo, UserInfoRequest userInfoRequest) throws IOException;
    void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest);
    UserDeleteResponse deleteUser(SignUserInfo signUserInfo);
}
