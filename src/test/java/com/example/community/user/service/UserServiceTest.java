package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private SignInfoRepository signInfoRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private String email;
    private String password;
    private String nickname;
    private UserDTO user;

    @BeforeEach
    void init(){
        email = "wns1628@gmail.com";
        password = "1234";
        nickname = "dave";
    }

    @Test
    @DisplayName("회원 가입 시 중복 이메일 입력 시 예외 발생")
    void signUpFailsWithDuplicateEmail() throws IOException {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 이메일 존재");
    }


    @Test
    @DisplayName("회원 가입 시 중복 닉네임 입력 시 예외 발생")
    void signUpFailsWithDuplicateNickname() {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("회원 가입 시 중복 비밀번호 확인 불일치 시 예외 발생")
    void signUpFailsWithPasswordConfirmMismatch() {
        String passwordConfirm = "12345";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("회원 가입 성공")
    void signUpSuccess() throws IOException {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(passwordEncoder.encode(password)).thenReturn(password);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        when(signInfoRepository.save(any(SignInfo.class)))
                .thenAnswer(invocation -> {
                    SignInfo signInfo = invocation.getArgument(0);

                    ReflectionTestUtils.setField(signInfo, "userNum", 1L);

                    return signInfo;
                });

        when(userInfoRepository.save(any(UserInfo.class)))
                .thenAnswer(invocation -> {
                    UserInfo userInfo = invocation.getArgument(0);

                    ReflectionTestUtils.setField(userInfo, "profileId", 1L);

                    return userInfo;
                });
        assertThat(userService.signUp(signUpRequest).userId()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 이메일 입력 시 로그인 실패")
    void signInFailsWhenEmailDoesNotExist() {
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, password))).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 이메일");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패")
    void signInFailsWhenPasswordDoesNotMatch() {
        String wrongPassword = "123";
        SignInfo signInfo = new SignInfo(1L, email, password, null, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, wrongPassword))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인 실패");
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인 시도 시 로그인 실패")
    void signInFailsWhenUserIsDeleted() {
        Instant now = Instant.now();
        SignInfo signInfo = new SignInfo(1L, email, password, now, null);

        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, password))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("탈퇴한 유저");
    }

    @Test
    @DisplayName("로그인 성공")
    void signInSuccess() {
        SignInfo signInfo = new SignInfo(1L, email, password, null, null);
        UserInfo userInfo = new UserInfo(1L, signInfo,nickname, null, UserRole.USER, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        when(passwordEncoder.matches(password,password)).thenReturn(true);
        when(userInfoRepository.findBySignInfo_UserNum(1L)).thenReturn(Collections.singletonList(userInfo));
        assertThat(userService.signIn(new SignInRequest(email, password)))
                .usingRecursiveComparison()
                .isEqualTo(UserInfoDTO.from(userInfo));;
    }

    @Test
    @DisplayName("유저 정보 수정 시 존재하지 않는 프로필 번호 입력 시 실패")
    void updateUserInfoFailsWhenProfileDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.empty());
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);

        assertThatThrownBy(() -> userService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("유저 정보 수정 시 중복 닉네임 입력 시 실패")
    void updateUserInfoFailsWhenNicknameIsDuplicated() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", null)));
        when(userInfoRepository.existsByNickname(any(String.class))).thenReturn(true);
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);

        assertThatThrownBy(() -> userService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("유저 정보 수정 성공")
    void updateUserInfoSuccess() throws IOException {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", null)));
        when(userInfoRepository.existsByNickname(any(String.class))).thenReturn(false);
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);
        UserInfoDTO updatedUserInfoDTO = new UserInfoDTO(1L, 1L, "wns1628@gmail.com", "dave2", null, UserRole.USER, null);

        assertThat(userService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(updatedUserInfoDTO));
    }

    @Test
    @DisplayName("비밀번호 변경 시 존재하지 않는 유저이면 실패")
    void changePasswordFailsWhenUserDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 일치하지 않으면 실패")
    void changePasswordFailsWhenCurrentPasswordDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("wrong-password", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(new SignInfo("wns1628@gmail.com", "1234")));

        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호가 틀렸습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 확인이 일치하지 않으면 실패")
    void changePasswordFailsWhenNextPasswordConfirmDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "123456");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(new SignInfo("wns1628@gmail.com", "1234")));

        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", "1234");

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(signInfo));

        userService.changePassword(signUserInfo, passwordChangeRequest);
        assertThat(signInfo.getPassword()).isEqualTo("12345");
    }

    @Test
    @DisplayName("유저 삭제 성공")
    void deleteUserSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        UserDeleteResponse userDeleteResponse =
                new UserDeleteResponse(signUserInfo.userNum(), true);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", "1234");
        when(signInfoRepository.findByUserNum(1L)).thenReturn(Optional.of(signInfo));
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(new UserInfo(signInfo, "dave", null)));

        assertThat(userService.deleteUser(signUserInfo))
                .usingRecursiveComparison()
                .isEqualTo(userDeleteResponse);
    }
}