package com.example.community.user.controller;

import com.example.community.TestResolverConfig;
import com.example.community.configuration.WebConfig;
import com.example.community.auth.service.RefreshTokenService;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.UserRole;
import com.example.community.user.service.UserService;
import com.example.community.util.JWTUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = SignUserArgumentResolver.class
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

    @MockitoBean
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("회원가입 성공")
    void signUpSuccess() throws Exception {
        SignUpRequest request = new SignUpRequest(
                "wns1628@gmail.com",
                "1234qwer",
                "1234qwer",
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
        String email = "wns1628@gmail.com";
        String password = "1234qwer";
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        SignInRequest request = new SignInRequest(email, password);

        UserInfoDTO userInfoDTO = new UserInfoDTO(
                1L,
                1L,
                email,
                "dave",
                null,
                UserRole.USER,
                null
        );

        when(userService.signIn(request)).thenReturn(userInfoDTO);
        when(jwtUtil.generateAccessToken(
                userInfoDTO.getUserNum(),
                userInfoDTO.getProfileId(),
                userInfoDTO.getUserRole()
        )).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(userInfoDTO.getUserNum()))
                .thenReturn(refreshToken);

        mockMvc.perform(post("/users/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh"))
                .andExpect(cookie().httpOnly("refresh", true))
                .andExpect(jsonPath("$.code").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));

        verify(userService).signIn(request);
        verify(jwtUtil).generateAccessToken(
                userInfoDTO.getUserNum(),
                userInfoDTO.getProfileId(),
                userInfoDTO.getUserRole()
        );
        verify(jwtUtil).generateRefreshToken(userInfoDTO.getUserNum());
        verify(refreshTokenService).addRefreshToken(userInfoDTO.getUserNum(), refreshToken);
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
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() throws Exception {
        PasswordChangeRequest request =
                new PasswordChangeRequest("qwer1234", "qwer12345", "qwer12345");

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
    @DisplayName("회원탈퇴 성공")
    void deleteUserSuccess() throws Exception {
        UserDeleteResponse response = new UserDeleteResponse(1L, true);

        when(userService.deleteUser(any(SignUserInfo.class)))
                .thenReturn(response);

        mockMvc.perform(delete("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("회원탈퇴 완료"))
                .andExpect(jsonPath("$.data.userNum").value(response.userNum()))
                .andExpect(jsonPath("$.data.deleted").value(response.deleted()));

        verify(userService).deleteUser(any(SignUserInfo.class));
    }
}