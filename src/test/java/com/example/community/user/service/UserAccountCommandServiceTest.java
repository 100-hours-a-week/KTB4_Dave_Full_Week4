package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.event.UserDisplayChangedEvent;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserAccountCommandServiceTest {
    @Mock
    private SignInfoRepository signInfoRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ImageConverter imageConverter;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private UserAccountCommandService accountCommandService;

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
    @DisplayName("회원 가입 시 중복 이메일 입력 시 예외 발생")
    void signUpFailsWithDuplicateEmail() {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> accountCommandService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 이메일 존재");
    }


    @Test
    @DisplayName("회원 가입 시 중복 닉네임 입력 시 예외 발생")
    void signUpFailsWithDuplicateNickname() {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(true);

        assertThatThrownBy(() -> accountCommandService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("회원 가입 시 중복 비밀번호 확인 불일치 시 예외 발생")
    void signUpFailsWithPasswordConfirmMismatch() {
        String passwordConfirm = "12345";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        assertThatThrownBy(() -> accountCommandService.signUp(signUpRequest)).isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("회원 가입 성공")
    void signUpSuccess(){
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
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
        when(imageConverter.updateProfileImage(null)).thenReturn(null);
        assertThat(accountCommandService.signUp(signUpRequest).userId()).isEqualTo(1);
    }













    @Test
    @DisplayName("비밀번호 변경 시 존재하지 않는 유저이면 실패")
    void changePasswordFailsWhenUserDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountCommandService.changePassword(signUserInfo, passwordChangeRequest))
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

        assertThatThrownBy(() -> accountCommandService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호가 틀렸습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 확인이 일치하지 않으면 실패")
    void changePasswordFailsWhenNextPasswordConfirmDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest(password, "12345", "123456");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(new SignInfo("wns1628@gmail.com", encodedPassword)));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);


        assertThatThrownBy(() -> accountCommandService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", encodedPassword);

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest(password, "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(signInfo));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(passwordEncoder.encode(passwordChangeRequest.nextPassword())).thenReturn("encodedPassword2");

        System.out.println(signInfo.getPassword());
        accountCommandService.changePassword(signUserInfo, passwordChangeRequest);
        assertThat(signInfo.getPassword()).isEqualTo("encodedPassword2");
    }




    @Test
    @DisplayName("유저 삭제 시 존재하지 않는 프로필 번호이면 실패")
    void deleteUserFailsWhenProfileIdDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(signUserInfo.profileId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountCommandService.deleteUser(signUserInfo))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("유저 삭제 성공")
    void deleteUserSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        UserDeleteResponse userDeleteResponse =
                new UserDeleteResponse(signUserInfo.userNum(), true);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", "1234");
        signInfo.setUserNum(signUserInfo.userNum());
        UserInfo userInfo = new UserInfo(signInfo, "dave", null);
        when(userInfoRepository.findByProfileId(signUserInfo.profileId()))
                .thenReturn(Optional.of(userInfo));

        assertThat(accountCommandService.deleteUser(signUserInfo))
                .usingRecursiveComparison()
                .isEqualTo(userDeleteResponse);
        assertThat(userInfo.isDeleted()).isTrue();
        assertThat(signInfo.isDeleted()).isTrue();
        verifyNoInteractions(signInfoRepository);
        verify(eventPublisher).publishEvent(new UserDisplayChangedEvent(1L));
    }
}
