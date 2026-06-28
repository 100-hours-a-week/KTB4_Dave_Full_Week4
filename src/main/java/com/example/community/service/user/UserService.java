package com.example.community.service.user;

import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.SignUpResponse;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.resolver.SignUserInfo;

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
