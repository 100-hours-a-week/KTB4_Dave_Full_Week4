package com.example.community.service;

import com.example.community.domain.token.Token;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.domain.user.response.UserResponse;

public interface UserService {
    long signUp(SignUpRequest signUpRequest);
    UserResponse signIn(SignInRequest signInRequest);
    void signOut(long userNum);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoResponse updateUserInfo(Token token, UserInfoRequest userInfoRequest);
    void changePassword(Token token, PasswordChangeRequest passwordChangeRequest);
    UserDeleteResponse deleteUser(Token token);
}
