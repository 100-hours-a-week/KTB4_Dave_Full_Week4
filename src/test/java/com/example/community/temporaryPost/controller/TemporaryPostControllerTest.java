package com.example.community.temporaryPost.controller;

import com.example.community.TestResolverConfig;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.request.TemporaryPostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.service.TemporaryPostCommandService;
import com.example.community.temporaryPost.service.TemporaryPostQueryService;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TemporaryPostController.class,
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
class TemporaryPostControllerTest {

    private static final SignUserInfo SIGN_USER_INFO =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final long TEMPORARY_ID = 10L;
    private static final String TITLE = "temporary-title";
    private static final String CONTENT = "temporary-content";
    private static final OffsetDateTime WRITE_AT =
            OffsetDateTime.parse("2026-08-05T09:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemporaryPostCommandService temporaryPostCommandService;

    @MockitoBean
    private TemporaryPostQueryService temporaryPostQueryService;

    @Test
    @DisplayName("첫 임시저장 시 내용을 저장하고 발급한 키를 반환한다")
    void createTemporaryPostSuccess() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "temporary.png",
                "image/png",
                new byte[]{1}
        );
        TemporaryPostRequest request = new TemporaryPostRequest(
                TITLE,
                CONTENT,
                image
        );
        when(temporaryPostCommandService.createTemporaryPost(
                SIGN_USER_INFO,
                request
        ))
                .thenReturn(new TemporaryKeyResponse(
                        TEMPORARY_ID,
                        "posts/temporary.png"
                ));

        performCreate(TITLE, CONTENT, image)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("임시저장 완료"))
                .andExpect(jsonPath("$.data.temporaryKeyId")
                        .value(TEMPORARY_ID))
                .andExpect(jsonPath("$.data.objectKey")
                        .value("posts/temporary.png"));

        verify(temporaryPostCommandService).createTemporaryPost(
                SIGN_USER_INFO,
                request
        );
    }

    @Test
    @DisplayName("첫 임시저장 시 사용자가 없으면 404 응답을 반환한다")
    void createTemporaryPostReturnsNotFoundWhenUserDoesNotExist()
            throws Exception {
        TemporaryPostRequest request = new TemporaryPostRequest(
                TITLE,
                CONTENT,
                null
        );
        when(temporaryPostCommandService.createTemporaryPost(
                SIGN_USER_INFO,
                request
        ))
                .thenThrow(new NotFoundException("존재하지 않는 유저"));

        performCreate(TITLE, CONTENT, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 유저"));

        verify(temporaryPostCommandService).createTemporaryPost(
                SIGN_USER_INFO,
                request
        );
    }

    @Test
    @DisplayName("첫 임시저장 시 제목이 공백이면 400 응답을 반환한다")
    void createTemporaryPostReturnsBadRequestWhenTitleIsBlank()
            throws Exception {
        performCreate(" ", CONTENT, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(temporaryPostCommandService, temporaryPostQueryService);
    }

    @Test
    @DisplayName("임시저장글 목록 조회 성공 응답을 반환한다")
    void getTemporaryPostsSuccess() throws Exception {
        List<TemporaryPostTitleResponse> response = List.of(
                new TemporaryPostTitleResponse(
                        TEMPORARY_ID,
                        TITLE,
                        WRITE_AT
                )
        );
        when(temporaryPostQueryService.getTemporaryPosts(SIGN_USER_INFO))
                .thenReturn(response);

        mockMvc.perform(get("/temporaryPost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("임시저장 게시글 목록 불러오기 성공"))
                .andExpect(jsonPath("$.data[0].temporaryPostId")
                        .value(TEMPORARY_ID))
                .andExpect(jsonPath("$.data[0].title").value(TITLE));

        verify(temporaryPostQueryService).getTemporaryPosts(SIGN_USER_INFO);
    }

    @Test
    @DisplayName("임시저장글 상세 조회 성공 응답을 반환한다")
    void getTemporaryPostSuccess() throws Exception {
        when(temporaryPostQueryService.getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        )).thenReturn(temporaryPostResponse());

        mockMvc.perform(get("/temporaryPost/{temporaryId}", TEMPORARY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("임시저장 게시글 불러오기 성공"))
                .andExpect(jsonPath("$.data.title").value(TITLE))
                .andExpect(jsonPath("$.data.content").value(CONTENT))
                .andExpect(jsonPath("$.data.objectKey")
                        .value("posts/temporary.png"));

        verify(temporaryPostQueryService).getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글 상세 조회 시 404 응답을 반환한다")
    void getTemporaryPostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        when(temporaryPostQueryService.getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        )).thenThrow(new NotFoundException("존재하지 않는 임시저장글"));

        mockMvc.perform(get("/temporaryPost/{temporaryId}", TEMPORARY_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("존재하지 않는 임시저장글"));

        verify(temporaryPostQueryService).getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    @Test
    @DisplayName("권한 없는 임시저장글 상세 조회 시 403 응답을 반환한다")
    void getTemporaryPostReturnsForbiddenWhenUserIsNotOwner() throws Exception {
        when(temporaryPostQueryService.getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        )).thenThrow(new ForbiddenException("접근권한 부족"));

        mockMvc.perform(get("/temporaryPost/{temporaryId}", TEMPORARY_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근권한 부족"));

        verify(temporaryPostQueryService).getTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    @Test
    @DisplayName("이미지를 포함한 임시저장글 수정 성공 응답을 반환한다")
    void updateTemporaryPostSuccess() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "temporary.png",
                "image/png",
                new byte[]{1}
        );
        PostUpdateRequest request = new PostUpdateRequest(
                TITLE,
                CONTENT,
                null,
                image
        );
        when(temporaryPostCommandService.updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        )).thenReturn(temporaryPostResponse());

        performUpdate(TITLE, CONTENT, image)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("임시저장 완료"))
                .andExpect(jsonPath("$.data.title").value(TITLE))
                .andExpect(jsonPath("$.data.image")
                        .value("https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/posts/temporary.png"))
                .andExpect(jsonPath("$.data.objectKey")
                        .value("posts/temporary.png"));

        verify(temporaryPostCommandService).updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        );
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글 수정 시 404 응답을 반환한다")
    void updateTemporaryPostReturnsNotFoundWhenPostDoesNotExist()
            throws Exception {
        PostUpdateRequest request = new PostUpdateRequest(
                TITLE,
                CONTENT,
                "posts/temporary.png",
                null
        );
        when(temporaryPostCommandService.updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        )).thenThrow(new NotFoundException("존재하지 않는 임시저장글"));

        performUpdate(TITLE, CONTENT, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("존재하지 않는 임시저장글"));

        verify(temporaryPostCommandService).updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        );
    }

    @Test
    @DisplayName("권한 없는 임시저장글 수정 시 403 응답을 반환한다")
    void updateTemporaryPostReturnsForbiddenWhenUserIsNotOwner()
            throws Exception {
        PostUpdateRequest request = new PostUpdateRequest(
                TITLE,
                CONTENT,
                "posts/temporary.png",
                null
        );
        when(temporaryPostCommandService.updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        )).thenThrow(new ForbiddenException("접근권한 부족"));

        performUpdate(TITLE, CONTENT, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근권한 부족"));

        verify(temporaryPostCommandService).updateTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID,
                request
        );
    }

    @Test
    @DisplayName("임시저장글 수정 시 제목이 공백이면 400 응답을 반환한다")
    void updateTemporaryPostReturnsBadRequestWhenTitleIsBlank()
            throws Exception {
        performUpdate(" ", CONTENT, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(temporaryPostCommandService, temporaryPostQueryService);
    }

    @Test
    @DisplayName("임시저장글 삭제 성공 응답을 반환한다")
    void deleteTemporaryPostSuccess() throws Exception {
        doNothing().when(temporaryPostCommandService)
                .deleteTemporaryPost(SIGN_USER_INFO, TEMPORARY_ID);

        mockMvc.perform(delete(
                        "/temporaryPost/{temporaryId}",
                        TEMPORARY_ID
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("임시저장 게시글 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(temporaryPostCommandService).deleteTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글 삭제 시 404 응답을 반환한다")
    void deleteTemporaryPostReturnsNotFoundWhenPostDoesNotExist()
            throws Exception {
        doThrow(new NotFoundException("존재하지 않는 임시저장글"))
                .when(temporaryPostCommandService)
                .deleteTemporaryPost(SIGN_USER_INFO, TEMPORARY_ID);

        mockMvc.perform(delete(
                        "/temporaryPost/{temporaryId}",
                        TEMPORARY_ID
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("존재하지 않는 임시저장글"));

        verify(temporaryPostCommandService).deleteTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    @Test
    @DisplayName("권한 없는 임시저장글 삭제 시 403 응답을 반환한다")
    void deleteTemporaryPostReturnsForbiddenWhenUserIsNotOwner()
            throws Exception {
        doThrow(new ForbiddenException("접근권한 부족"))
                .when(temporaryPostCommandService)
                .deleteTemporaryPost(SIGN_USER_INFO, TEMPORARY_ID);

        mockMvc.perform(delete(
                        "/temporaryPost/{temporaryId}",
                        TEMPORARY_ID
                ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근권한 부족"));

        verify(temporaryPostCommandService).deleteTemporaryPost(
                SIGN_USER_INFO,
                TEMPORARY_ID
        );
    }

    private TemporaryPostResponse temporaryPostResponse() {
        return new TemporaryPostResponse(
                TITLE,
                CONTENT,
                "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/posts/temporary.png",
                "posts/temporary.png",
                WRITE_AT
        );
    }

    private ResultActions performCreate(
            String title,
            String content,
            MockMultipartFile image
    ) throws Exception {
        var requestBuilder = multipart("/temporaryPost")
                .param("title", title)
                .param("content", content);

        if (image != null) {
            requestBuilder.file(image);
        }
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions performUpdate(
            String title,
            String content,
            MockMultipartFile image
    ) throws Exception {
        String objectKey = image == null ? "posts/temporary.png" : null;
        return performUpdate(title, content, objectKey, image);
    }

    private ResultActions performUpdate(
            String title,
            String content,
            String objectKey,
            MockMultipartFile image
    ) throws Exception {
        var requestBuilder = multipart(
                "/temporaryPost/{temporaryId}",
                TEMPORARY_ID
        ).param("title", title)
                .param("content", content)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });

        if (objectKey != null) {
            requestBuilder.param("objectKey", objectKey);
        }

        if (image != null) {
            requestBuilder.file(image);
        }
        return mockMvc.perform(requestBuilder);
    }
}
