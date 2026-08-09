package com.example.community.auth.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialAuthenticator {
    private final SignInfoRepository signInfoRepository;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfoDTO authenticate(SignInRequest request) {
        SignInfo signInfo = signInfoRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 이메일")
                );
        if (!passwordEncoder.matches(
                request.password(),
                signInfo.getPassword()
        )) {
            throw new UnAuthorizedException("로그인 실패");
        }
        if (signInfo.isDeleted()) {
            throw new UnAuthorizedException("탈퇴한 유저");
        }

        UserInfoDTO userInfo = UserInfoDTO.from(userInfoRepository
                .findBySignInfo_UserNum(signInfo.getUserNum())
                .getFirst());
        userInfo.setEmail(signInfo.getEmail());
        signInfo.loginSuccess();
        return userInfo;
    }
}
