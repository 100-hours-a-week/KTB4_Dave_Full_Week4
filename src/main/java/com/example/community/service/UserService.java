package com.example.community.service;

import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.SignUpResponse;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.domain.user.response.UserResponse;

public interface UserService {
    SignUpResponse signUp(SignUpRequest signUpRequest);
    UserResponse signIn(SignInRequest signInRequest);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoResponse updateUserInfo(String token, UserInfoRequest userInfoRequest);
    void changePassword(String  token, PasswordChangeRequest passwordChangeRequest);
    UserDeleteResponse deleteUser(String token);
}
