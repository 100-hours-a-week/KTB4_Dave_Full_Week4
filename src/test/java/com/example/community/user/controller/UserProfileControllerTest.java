package com.example.community.user.controller;

import com.example.community.TestResolverConfig;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.UserRole;
import com.example.community.user.service.UserProfileCommandService;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserProfileController.class,
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
class UserProfileControllerTest {

    private static final String EMAIL = "wns1628@gmail.com";
    private static final String PASSWORD = "Password1!";
    private static final String NEXT_PASSWORD = "NewPassword2@";
    private static final SignUserInfo SIGN_USER_INFO =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final String IMAGE_BASE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";
    private static final ImageUrlBuilder IMAGE_URL_BUILDER =
            new ImageUrlBuilder(IMAGE_BASE_URL);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileCommandService profileCommandService;




















    @Test
    @DisplayName("회원정보 수정 성공")
    void updateInfoSuccess() throws Exception {
        UserInfoRequest request = new UserInfoRequest(
                "dave2",
                "profiles/profile.png",
                null
        );

        UserInfoResponse response = new UserInfoResponse(
                "dave2",
                "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/profiles/profile.png",
                "profiles/profile.png"
        );

        when(profileCommandService.updateUserInfo(SIGN_USER_INFO, request))
                .thenReturn(response);

        mockMvc.perform(multipart("/users/info")
                        .param("nickname", request.nickname())
                        .param("objectKey", request.objectKey())
                        .with(mockRequest -> {
                            mockRequest.setMethod("PATCH");
                            return mockRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("회원정보 수정 완료"))
                .andExpect(jsonPath("$.data.nickname").value(response.nickname()))
                .andExpect(jsonPath("$.data.profileImage").value(response.profileImage()))
                .andExpect(jsonPath("$.data.objectKey").value(response.objectKey()));

        verify(profileCommandService).updateUserInfo(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("회원정보 수정 시 다른 사용자의 닉네임과 중복되면 409 응답")
    void updateInfoReturnsConflictWhenNicknameIsDuplicated() throws Exception {
        UserInfoRequest request = new UserInfoRequest(
                "other",
                "profiles/profile.png",
                null
        );
        when(profileCommandService.updateUserInfo(SIGN_USER_INFO, request))
                .thenThrow(new DuplicateException("중복 닉네임 존재"));

        performUpdateInfo(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("중복 닉네임 존재"));

        verify(profileCommandService).updateUserInfo(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("회원정보 수정 시 닉네임 길이가 10자를 초과하면 400 응답")
    void updateInfoReturnsBadRequestWhenNicknameIsTooLong() throws Exception {
        UserInfoRequest request = new UserInfoRequest(
                "12345678901",
                "profiles/profile.png",
                null
        );

        performUpdateInfo(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith("입력데이터가 유효하지 않습니다.")));

        verifyNoInteractions(profileCommandService);
    }

    @Test
    @DisplayName("회원정보 수정 시 objectKey와 파일을 같이 보내면 400 응답")
    void updateInfoReturnsBadRequestForObjectKeyAndImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "imageFile",
                "profile.png",
                "image/png",
                new byte[]{1}
        );
        UserInfoRequest request = new UserInfoRequest(
                "dave2",
                "profiles/profile.png",
                image
        );

        performUpdateInfo(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(profileCommandService);
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
        var requestBuilder = multipart("/users/info")
                .param("nickname", request.nickname())
                .with(mockRequest -> {
                    mockRequest.setMethod("PATCH");
                    return mockRequest;
                });
        if (request.objectKey() != null) {
            requestBuilder.param("objectKey", request.objectKey());
        }
        if (request.imageFile() instanceof MockMultipartFile image) {
            requestBuilder.file(image);
        }
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performChangePassword(
            PasswordChangeRequest request
    ) throws Exception {
        return mockMvc.perform(patch("/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
