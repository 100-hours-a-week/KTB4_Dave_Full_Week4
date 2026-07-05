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
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserJpaServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserJpaService userJpaService;



    @Test
    @DisplayName("회원 가입 시 중복 이메일 입력 시 예외 발생")
    void signUpFailsWithDuplicateEmail() throws IOException {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String passwordConfirm = "1234";
        String nickname = "dave";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(userRepository.isExistEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userJpaService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 이메일 존재");
    }


    @Test
    @DisplayName("회원 가입 시 중복 닉네임 입력 시 예외 발생")
    void signUpFailsWithDuplicateNickname() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String passwordConfirm = "1234";
        String nickname = "dave";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(userRepository.isExistEmail(email)).thenReturn(false);
        when(userRepository.isExistNickname(nickname)).thenReturn(true);

        assertThatThrownBy(() -> userJpaService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("회원 가입 시 중복 비밀번호 확인 불일치 시 예외 발생")
    void signUpFailsWithPasswordConfirmMismatch() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String passwordConfirm = "12345";
        String nickname = "dave";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(userRepository.isExistEmail(email)).thenReturn(false);
        when(userRepository.isExistNickname(nickname)).thenReturn(false);

        assertThatThrownBy(() -> userJpaService.signUp(signUpRequest)).isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("회원 가입 성공")
    void signUpSuccess() throws IOException {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String passwordConfirm = "1234";
        String nickname = "dave";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(userRepository.isExistEmail(email)).thenReturn(false);
        when(userRepository.isExistNickname(nickname)).thenReturn(false);
        UserDTO user = new UserDTO(1L, 1L, email, password, nickname, null, null, UserRole.USER);
        when(userRepository.addUser(any(UserDTO.class))).thenReturn(user);

        assertThat(userJpaService.signUp(signUpRequest)).isInstanceOf(SignUpResponse.class);
        assertThat(userJpaService.signUp(signUpRequest).userId()).isEqualTo(user.getUserNum());
    }

    @Test
    @DisplayName("존재하지 않는 이메일 입력 시 로그인 실패")
    void signInFailsWhenEmailDoesNotExist() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userJpaService.signIn(new SignInRequest(email, password))).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 이메일");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패")
    void signInFailsWhenPasswordDoesNotMatch() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String nickname = "dave";
        UserDTO user = new UserDTO(1L, 1L, email, password, nickname, null, null, UserRole.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> userJpaService.signIn(new SignInRequest(email, "123"))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인 실패");
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인 시도 시 로그인 실패")
    void signInFailsWhenUserIsDeleted() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String nickname = "dave";
        UserDTO user = new UserDTO(1L, 1L, email, password, nickname, null, Instant.now(), UserRole.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> userJpaService.signIn(new SignInRequest(email, password))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("탈퇴한 유저");
    }

    @Test
    @DisplayName("로그인 성공")
    void signInSuccess() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String nickname = "dave";
        UserDTO user = new UserDTO(1L, 1L, email, password, nickname, null, null, UserRole.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThat(userJpaService.signIn(new SignInRequest(email, password)))
                .usingRecursiveComparison()
                .isEqualTo(UserInfoDTO.from(user));;
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void isExistEmail() {
        String email = "wns1628@gmail.com";
        when(userRepository.isExistEmail(email)).thenReturn(true);
        assertThat(userJpaService.isExistEmail(email)).isTrue();
        when(userRepository.isExistEmail(email)).thenReturn(false);
        assertThat(userJpaService.isExistEmail(email)).isFalse();
    }

    @Test
    @DisplayName("닉네임 중복 확인")
    void isExistNickname() {
        String nickname = "dave";
        when(userRepository.isExistNickname(nickname)).thenReturn(true);
        assertThat(userJpaService.isExistNickname(nickname)).isTrue();
        when(userRepository.isExistNickname(nickname)).thenReturn(false);
        assertThat(userJpaService.isExistNickname(nickname)).isFalse();
    }

    @Test
    @DisplayName("유저 정보 수정 시 존재하지 않는 프로필 번호 입력 시 실패")
    void updateUserInfoFailsWhenProfileDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userRepository.findByProfileId(1L)).thenReturn(Optional.empty());
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);

        assertThatThrownBy(() -> userJpaService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("유저 정보 수정 시 중복 닉네임 입력 시 실패")
    void updateUserInfoFailsWhenNicknameIsDuplicated() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfoDTO userInfoDTO = new UserInfoDTO(1L, 1L, "wns1628@gmail.com", "dave", null, UserRole.USER, null);
        when(userRepository.findByProfileId(1L)).thenReturn(Optional.of(userInfoDTO));
        when(userRepository.isExistNickname(any(String.class))).thenReturn(true);
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);

        assertThatThrownBy(() -> userJpaService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("유저 정보 수정 성공")
    void updateUserInfoSuccess() throws IOException {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfoDTO userInfoDTO = new UserInfoDTO(1L, 1L, "wns1628@gmail.com", "dave", "test.png", UserRole.USER, null);
        when(userRepository.findByProfileId(1L)).thenReturn(Optional.of(userInfoDTO));
        when(userRepository.isExistNickname(any(String.class))).thenReturn(false);
        UserInfoRequest userInfoRequest = new UserInfoRequest("dave2", null);
        UserInfoDTO updatedUserInfoDTO = new UserInfoDTO(1L, 1L, "wns1628@gmail.com", "dave2", null, UserRole.USER, null);
        when(userRepository.updateUserInfo(1L, "dave2", null)).thenReturn(updatedUserInfoDTO);

        assertThat(userJpaService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(updatedUserInfoDTO));
    }

    @Test
    @DisplayName("비밀번호 변경 시 존재하지 않는 유저이면 실패")
    void changePasswordFailsWhenUserDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(userRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userJpaService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 일치하지 않으면 실패")
    void changePasswordFailsWhenCurrentPasswordDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserDTO user = new UserDTO(
                1L,
                1L,
                "wns1628@gmail.com",
                "1234",
                "dave",
                null,
                null,
                UserRole.USER
        );

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("wrong-password", "12345", "12345");

        when(userRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userJpaService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호가 틀렸습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 확인이 일치하지 않으면 실패")
    void changePasswordFailsWhenNextPasswordConfirmDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserDTO user = new UserDTO(
                1L,
                1L,
                "wns1628@gmail.com",
                "1234",
                "dave",
                null,
                null,
                UserRole.USER
        );

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "123456");

        when(userRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userJpaService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserDTO user = new UserDTO(
                1L,
                1L,
                "wns1628@gmail.com",
                "1234",
                "dave",
                null,
                null,
                UserRole.USER
        );

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(userRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(user));

        userJpaService.changePassword(signUserInfo, passwordChangeRequest);

        verify(userRepository).changePassword(
                signUserInfo.userNum(),
                passwordChangeRequest.nextPassword()
        );
    }

    @Test
    @DisplayName("유저 삭제 성공")
    void deleteUserSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        UserDeleteResponse userDeleteResponse =
                new UserDeleteResponse(signUserInfo.userNum(), true);

        when(userRepository.deleteUser(signUserInfo.userNum()))
                .thenReturn(Instant.now());

        assertThat(userJpaService.deleteUser(signUserInfo))
                .usingRecursiveComparison()
                .isEqualTo(userDeleteResponse);
    }
}