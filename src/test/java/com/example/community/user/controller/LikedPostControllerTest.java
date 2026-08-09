package com.example.community.user.controller;

import com.example.community.TestResolverConfig;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.entity.UserRole;
import com.example.community.user.service.LikedPostQueryService;
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

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LikedPostController.class,
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
class LikedPostControllerTest {

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
    private LikedPostQueryService likedPostQueryService;


















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
        when(likedPostQueryService.getMyLikePosts(SIGN_USER_INFO, 0, 10, "latest"))
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

        verify(likedPostQueryService).getMyLikePosts(SIGN_USER_INFO, 0, 10, "latest");
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
        when(likedPostQueryService.getMyLikePosts(SIGN_USER_INFO, 2, 5, "likes"))
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

        verify(likedPostQueryService).getMyLikePosts(SIGN_USER_INFO, 2, 5, "likes");
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
