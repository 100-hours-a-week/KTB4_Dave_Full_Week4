package com.example.community.auth.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CredentialAuthenticatorTest {
    @Mock
    private SignInfoRepository signInfoRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CredentialAuthenticator credentialAuthenticator;

    private String email;
    private String encodedPassword;
    private String nickname;
    private String password;

    @BeforeEach
    void init(){
        email = "wns1628@gmail.com";
        password = "1234";
        encodedPassword = "encoded_password";
        nickname = "dave";
    }








    @Test
    @DisplayName("존재하지 않는 이메일 입력 시 로그인 실패")
    void signInFailsWhenEmailDoesNotExist() {
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialAuthenticator.authenticate(new SignInRequest(email, password))).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 이메일");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패")
    void signInFailsWhenPasswordDoesNotMatch() {
        String wrongPassword = "123";
        SignInfo signInfo = new SignInfo(1L, email, encodedPassword, null, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> credentialAuthenticator.authenticate(new SignInRequest(email, wrongPassword))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인 실패");
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인 시도 시 로그인 실패")
    void signInFailsWhenUserIsDeleted() {
        Instant now = Instant.now();
        SignInfo signInfo = new SignInfo(1L, email, password, now, null);

        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> credentialAuthenticator.authenticate(new SignInRequest(email, password))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("탈퇴한 유저");
    }

    @Test
    @DisplayName("로그인 성공")
    void signInSuccess() {
        SignInfo signInfo = new SignInfo(1L, email, encodedPassword, null, null);
        UserInfo userInfo = new UserInfo(1L, signInfo,nickname, null, UserRole.USER, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(userInfoRepository.findBySignInfo_UserNum(1L)).thenReturn(Collections.singletonList(userInfo));
        UserInfoDTO userInfoDTO = UserInfoDTO.from(userInfo);
        userInfoDTO.setEmail(email);
        assertThat(credentialAuthenticator.authenticate(new SignInRequest(email, password)))
                .usingRecursiveComparison()
                .isEqualTo(userInfoDTO);
    }

















}
