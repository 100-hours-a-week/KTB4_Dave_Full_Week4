package com.example.community.user.controller;

import com.example.community.TestResolverConfig;
import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.service.AuthService;
import com.example.community.auth.service.RefreshTokenService;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignInResponse;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.UserRole;
import com.example.community.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                SignUserArgumentResolver.class,
                                JwtFilter.class,
                                RateLimitFilter.class
                        }
                ),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebConfig.class
                )
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestResolverConfig.class)
class UserControllerTest {

    private static final String EMAIL = "wns1628@gmail.com";
    private static final String PASSWORD = "Password1!";
    private static final String NEXT_PASSWORD = "NewPassword2@";
    private static final SignUserInfo SIGN_USER_INFO =
            new SignUserInfo(1L, 1L, UserRole.USER);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean(name = "userService")
    private UserService userService;

    @MockitoBean(name = "refreshTokenService")
    private RefreshTokenService refreshTokenService;

    @MockitoBean(name = "authService")
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signUpSuccess() throws Exception {
        SignUpRequest request = new SignUpRequest(
                EMAIL,
                PASSWORD,
                PASSWORD,
                "dave",
                null
        );

        SignUpResponse response = new SignUpResponse(1L);

        when(userService.signUp(request)).thenReturn(response);

        mockMvc.perform(multipart("/users")
                        .param("email", request.email())
                        .param("password", request.password())
                        .param("passwordConfirm", request.passwordConfirm())
                        .param("nickname", request.nickname()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/users/state"))
                .andExpect(jsonPath("$.code").value("회원가입 성공"))
                .andExpect(jsonPath("$.data.userId").value(response.userId()));

        verify(userService).signUp(request);
    }

    @Test
    @DisplayName("회원가입 시 이메일 형식이 올바르지 않으면 400 응답")
    void signUpReturnsBadRequestWhenEmailFormatIsInvalid() throws Exception {
        SignUpRequest request = new SignUpRequest(
                "invalid-email",
                PASSWORD,
                PASSWORD,
                "dave",
                null
        );

        performSignUp(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("회원가입 시 이메일이 중복되면 409 응답")
    void signUpReturnsConflictWhenEmailIsDuplicated() throws Exception {
        SignUpRequest request = signUpRequest("dave", PASSWORD, PASSWORD);
        when(userService.signUp(request))
                .thenThrow(new DuplicateException("중복 이메일 존재"));

        performSignUp(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 이메일 존재"));

        verify(userService).signUp(request);
    }

    @Test
    @DisplayName("회원가입 시 닉네임 길이가 10자를 초과하면 400 응답")
    void signUpReturnsBadRequestWhenNicknameIsTooLong() throws Exception {
        SignUpRequest request = signUpRequest("12345678901", PASSWORD, PASSWORD);

        performSignUp(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("회원가입 시 닉네임이 중복되면 409 응답")
    void signUpReturnsConflictWhenNicknameIsDuplicated() throws Exception {
        SignUpRequest request = signUpRequest("dave", PASSWORD, PASSWORD);
        when(userService.signUp(request))
                .thenThrow(new DuplicateException("중복 닉네임 존재"));

        performSignUp(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 닉네임 존재"));

        verify(userService).signUp(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password1!",
            "PASSWORD1!",
            "Password!!",
            "Password12",
            "Password 1!"
    })
    @DisplayName("회원가입 시 비밀번호 형식이 올바르지 않으면 400 응답")
    void signUpReturnsBadRequestWhenPasswordFormatIsInvalid(
            String invalidPassword
    ) throws Exception {
        SignUpRequest request = signUpRequest("dave", invalidPassword, invalidPassword);

        performSignUp(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Aa1!",
            "Password1234567890!Ab"
    })
    @DisplayName("회원가입 시 비밀번호 길이가 8~20자 범위를 벗어나면 400 응답")
    void signUpReturnsBadRequestWhenPasswordLengthIsInvalid(
            String invalidPassword
    ) throws Exception {
        SignUpRequest request = signUpRequest("dave", invalidPassword, invalidPassword);

        performSignUp(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호 확인이 일치하지 않으면 400 응답")
    void signUpReturnsBadRequestWhenPasswordConfirmDoesNotMatch() throws Exception {
        SignUpRequest request = signUpRequest("dave", PASSWORD, NEXT_PASSWORD);
        when(userService.signUp(request))
                .thenThrow(new BadRequestException("비밀번호 확인 불일치"));

        performSignUp(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("비밀번호 확인 불일치"));

        verify(userService).signUp(request);
    }

    @Test
    @DisplayName("이메일 중복 확인 시 중복이면 409 응답")
    void checkEmailDuplicateReturnsConflictWhenEmailExists() throws Exception {
        String email = "wns1628@gmail.com";

        when(userService.isExistEmail(email)).thenReturn(true);

        mockMvc.perform(post("/users/email")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(email))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 이메일 존재"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).isExistEmail(email);
    }

    @Test
    @DisplayName("이메일 중복 확인 시 사용 가능하면 200 응답")
    void checkEmailDuplicateReturnsOkWhenEmailIsAvailable() throws Exception {
        String email = "wns1628@gmail.com";

        when(userService.isExistEmail(email)).thenReturn(false);

        mockMvc.perform(post("/users/email")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("가입 가능한 이메일"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).isExistEmail(email);
    }

    @Test
    @DisplayName("닉네임 중복 확인 시 중복이면 409 응답")
    void checkNicknameDuplicateReturnsConflictWhenNicknameExists() throws Exception {
        String nickname = "dave";

        when(userService.isExistNickname(nickname)).thenReturn(true);

        mockMvc.perform(post("/users/nickname")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(nickname))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 닉네임 존재"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).isExistNickname(nickname);
    }

    @Test
    @DisplayName("닉네임 중복 확인 시 사용 가능하면 200 응답")
    void checkNicknameDuplicateReturnsOkWhenNicknameIsAvailable() throws Exception {
        String nickname = "dave";

        when(userService.isExistNickname(nickname)).thenReturn(false);

        mockMvc.perform(post("/users/nickname")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(nickname))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("사용 가능한 닉네임"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).isExistNickname(nickname);
    }

    @Test
    @DisplayName("로그인 성공")
    void signInSuccess() throws Exception {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        SignInRequest request = new SignInRequest(EMAIL, PASSWORD);

        UserInfoDTO userInfoDTO = new UserInfoDTO(
                1L,
                1L,
                EMAIL,
                "dave",
                null,
                UserRole.USER,
                null
        );
        AuthResponse authResponse = new AuthResponse(refreshToken, SignInResponse.of(userInfoDTO, accessToken));

        when(userService.signIn(request)).thenReturn(userInfoDTO);
        when(authService.tokenIssue(userInfoDTO)).thenReturn(authResponse);

        mockMvc.perform(post("/users/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh"))
                .andExpect(cookie().httpOnly("refresh", true))
                .andExpect(jsonPath("$.code").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));

        verify(userService).signIn(request);
        verify(authService).tokenIssue(userInfoDTO);
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 이메일이면 404 응답")
    void signInReturnsNotFoundWhenEmailDoesNotExist() throws Exception {
        SignInRequest request = new SignInRequest(EMAIL, PASSWORD);
        when(userService.signIn(request))
                .thenThrow(new NotFoundException("존재하지 않는 이메일"));

        performSignIn(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 이메일"));

        verify(userService).signIn(request);
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 일치하지 않으면 401 응답")
    void signInReturnsUnauthorizedWhenPasswordDoesNotMatch() throws Exception {
        SignInRequest request = new SignInRequest(EMAIL, PASSWORD);
        when(userService.signIn(request))
                .thenThrow(new UnAuthorizedException("로그인 실패"));

        performSignIn(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("로그인 실패"));

        verify(userService).signIn(request);
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그인 시 이미 탈퇴한 유저이면 401 응답")
    void signInReturnsUnauthorizedWhenUserIsDeleted() throws Exception {
        SignInRequest request = new SignInRequest(EMAIL, PASSWORD);
        when(userService.signIn(request))
                .thenThrow(new UnAuthorizedException("탈퇴한 유저"));

        performSignIn(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("탈퇴한 유저"));

        verify(userService).signIn(request);
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void signOutSuccess() throws Exception {
        String refreshToken = "refresh-token";

        doNothing().when(refreshTokenService).deleteRefreshToken(refreshToken);

        mockMvc.perform(delete("/users/state")
                        .cookie(new jakarta.servlet.http.Cookie("refresh", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("로그아웃 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(refreshTokenService).deleteRefreshToken(refreshToken);
    }

    @Test
    @DisplayName("좋아요한 게시글 목록은 기본 조회 조건으로 불러온다")
    void getMyLikePostUsesDefaultRequestParameters() throws Exception {
        PostPageResponse response = new PostPageResponse(
                List.of(),
                0,
                10,
                0,
                0,
                0
        );
        when(userService.getMyLikePosts(SIGN_USER_INFO, 0, 10, "latest"))
                .thenReturn(response);

        mockMvc.perform(get("/users/myLike"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("좋아요 한 게시글 목록 불러오기 성공"))
                .andExpect(jsonPath("$.data.postTitleResponses").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.postCount").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.totalPage").value(0));

        verify(userService).getMyLikePosts(SIGN_USER_INFO, 0, 10, "latest");
    }

    @Test
    @DisplayName("좋아요한 게시글 목록은 요청한 조회 조건으로 불러온다")
    void getMyLikePostUsesRequestedParameters() throws Exception {
        PostPageResponse response = new PostPageResponse(
                List.of(),
                2,
                5,
                0,
                0,
                0
        );
        when(userService.getMyLikePosts(SIGN_USER_INFO, 2, 5, "likes"))
                .thenReturn(response);

        mockMvc.perform(get("/users/myLike")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("좋아요 한 게시글 목록 불러오기 성공"))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(userService).getMyLikePosts(SIGN_USER_INFO, 2, 5, "likes");
    }

    @Test
    @DisplayName("회원정보 수정 성공")
    void updateInfoSuccess() throws Exception {
        UserInfoRequest request = new UserInfoRequest("dave2", null);

        UserInfoResponse response = new UserInfoResponse(
                "dave2",
                null
        );

        when(userService.updateUserInfo(SIGN_USER_INFO, request))
                .thenReturn(response);

        mockMvc.perform(multipart("/users/info")
                        .param("nickname", request.nickname())
                        .with(mockRequest -> {
                            mockRequest.setMethod("PATCH");
                            return mockRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("회원정보 수정 완료"))
                .andExpect(jsonPath("$.data.nickname").value(response.nickname()))
                .andExpect(jsonPath("$.data.profileImage").value(response.profileImage()));

        verify(userService).updateUserInfo(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("회원정보 수정 시 다른 사용자의 닉네임과 중복되면 409 응답")
    void updateInfoReturnsConflictWhenNicknameIsDuplicated() throws Exception {
        UserInfoRequest request = new UserInfoRequest("other", null);
        when(userService.updateUserInfo(SIGN_USER_INFO, request))
                .thenThrow(new DuplicateException("중복 닉네임 존재"));

        performUpdateInfo(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 닉네임 존재"));

        verify(userService).updateUserInfo(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("회원정보 수정 시 닉네임 길이가 10자를 초과하면 400 응답")
    void updateInfoReturnsBadRequestWhenNicknameIsTooLong() throws Exception {
        UserInfoRequest request = new UserInfoRequest("12345678901", null);

        performUpdateInfo(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() throws Exception {
        PasswordChangeRequest request =
                new PasswordChangeRequest(PASSWORD, NEXT_PASSWORD, NEXT_PASSWORD);

        doNothing().when(userService)
                .changePassword(SIGN_USER_INFO, request);

        mockMvc.perform(patch("/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("비밀번호 변경 완료"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userService).changePassword(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 틀리면 400 응답")
    void changePasswordReturnsBadRequestWhenCurrentPasswordDoesNotMatch() throws Exception {
        PasswordChangeRequest request =
                new PasswordChangeRequest("WrongPassword3#", NEXT_PASSWORD, NEXT_PASSWORD);
        doThrow(new BadRequestException("비밀번호가 틀렸습니다."))
                .when(userService)
                .changePassword(SIGN_USER_INFO, request);

        performChangePassword(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("비밀번호가 틀렸습니다."));

        verify(userService).changePassword(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 형식이 올바르지 않으면 400 응답")
    void changePasswordReturnsBadRequestWhenNextPasswordFormatIsInvalid() throws Exception {
        String invalidPassword = "newpassword2@";
        PasswordChangeRequest request =
                new PasswordChangeRequest(PASSWORD, invalidPassword, invalidPassword);

        performChangePassword(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 확인이 일치하지 않으면 400 응답")
    void changePasswordReturnsBadRequestWhenNextPasswordConfirmDoesNotMatch() throws Exception {
        PasswordChangeRequest request =
                new PasswordChangeRequest(PASSWORD, NEXT_PASSWORD, "OtherPassword3#");
        doThrow(new BadRequestException("비밀번호 확인 불일치"))
                .when(userService)
                .changePassword(SIGN_USER_INFO, request);

        performChangePassword(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("비밀번호 확인 불일치"));

        verify(userService).changePassword(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("회원탈퇴 시 존재하지 않는 유저이면 404 응답")
    void deleteUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.deleteUser(SIGN_USER_INFO))
                .thenThrow(new NotFoundException("존재하지 않는 유저"));

        mockMvc.perform(delete("/users"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 유저"));

        verify(userService).deleteUser(SIGN_USER_INFO);
    }

    @Test
    @DisplayName("회원탈퇴 성공")
    void deleteUserSuccess() throws Exception {
        UserDeleteResponse response = new UserDeleteResponse(1L, true);

        when(userService.deleteUser(SIGN_USER_INFO))
                .thenReturn(response);

        mockMvc.perform(delete("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("회원탈퇴 완료"))
                .andExpect(jsonPath("$.data.userNum").value(response.userNum()))
                .andExpect(jsonPath("$.data.deleted").value(response.deleted()));

        verify(userService).deleteUser(SIGN_USER_INFO);
    }

    private SignUpRequest signUpRequest(
            String nickname,
            String password,
            String passwordConfirm
    ) {
        return new SignUpRequest(
                EMAIL,
                password,
                passwordConfirm,
                nickname,
                null
        );
    }

    private ResultActions performSignUp(
            SignUpRequest request
    ) throws Exception {
        return mockMvc.perform(multipart("/users")
                .param("email", request.email())
                .param("password", request.password())
                .param("passwordConfirm", request.passwordConfirm())
                .param("nickname", request.nickname()));
    }

    private ResultActions performSignIn(
            SignInRequest request
    ) throws Exception {
        return mockMvc.perform(post("/users/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performUpdateInfo(
            UserInfoRequest request
    ) throws Exception {
        return mockMvc.perform(multipart("/users/info")
                .param("nickname", request.nickname())
                .with(mockRequest -> {
                    mockRequest.setMethod("PATCH");
                    return mockRequest;
                }));
    }

    private ResultActions performChangePassword(
            PasswordChangeRequest request
    ) throws Exception {
        return mockMvc.perform(patch("/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
