package com.example.community.post.controller;

import com.example.community.TestResolverConfig;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.post.dto.response.*;
import com.example.community.post.service.PostCommandService;
import com.example.community.post.service.PostInteractionService;
import com.example.community.post.service.PostQueryService;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PostController.class,
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
class PostControllerTest {

    private static final SignUserInfo SIGN_USER_INFO =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final long POST_NUM = 10L;
    private static final String TITLE = "title";
    private static final String CONTENT = "content";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostCommandService postCommandService;

    @MockitoBean
    private PostQueryService postQueryService;

    @MockitoBean
    private PostInteractionService postInteractionService;

    @Test
    @DisplayName("게시글 목록은 기본 조회 조건으로 불러온다")
    void getPostsUsesDefaultRequestParameters() throws Exception {
        PostPageResponse response = postPageResponse(0, 10);
        when(postQueryService.getPostsByPage(0, 10, "latest"))
                .thenReturn(response);

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("게시글 조회 성공"))
                .andExpect(jsonPath("$.data.postTitleResponses").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        verify(postQueryService).getPostsByPage(0, 10, "latest");
    }

    @Test
    @DisplayName("게시글 목록은 요청한 조회 조건으로 불러온다")
    void getPostsUsesRequestedParameters() throws Exception {
        PostPageResponse response = postPageResponse(2, 5);
        when(postQueryService.getPostsByPage(2, 5, "likes"))
                .thenReturn(response);

        mockMvc.perform(get("/posts")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(postQueryService).getPostsByPage(2, 5, "likes");
    }

    @Test
    @DisplayName("공개 게시글 목록은 신고 수 대신 블라인드 여부를 반환한다")
    void getPostsReturnsBlindWithoutReportCount() throws Exception {
        PostTitleResponse post = new PostTitleResponse(
                POST_NUM,
                "author",
                null,
                "신고 처리된 글",
                10,
                3,
                2,
                true,
                OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
        );
        PostPageResponse response = new PostPageResponse(
                List.of(post),
                0,
                10,
                1,
                1,
                1
        );
        when(postQueryService.getPostsByPage(0, 10, "latest"))
                .thenReturn(response);

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postTitleResponses[0].blind")
                        .value(true))
                .andExpect(jsonPath("$.data.postTitleResponses[0].reportCount")
                        .doesNotExist());
    }

    @Test
    @DisplayName("게시글 상세 조회 성공")
    void getPostSuccess() throws Exception {
        PostDetailResponse response = postDetailResponse();
        when(postQueryService.getPost(SIGN_USER_INFO, POST_NUM))
                .thenReturn(response);

        mockMvc.perform(get("/posts/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("게시글 상세 조회 성공"))
                .andExpect(jsonPath("$.data.postNum").value(POST_NUM))
                .andExpect(jsonPath("$.data.title").value(TITLE))
                .andExpect(jsonPath("$.data.objectKey")
                        .value("posts/existing.png"))
                .andExpect(jsonPath("$.data.blind").value(false))
                .andExpect(jsonPath("$.data.reportCount").doesNotExist());

        verify(postQueryService).getPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 상세 조회 시 404 응답")
    void getPostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        when(postQueryService.getPost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        mockMvc.perform(get("/posts/{postNum}", POST_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postQueryService).getPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("블라인드된 게시글 상세 조회 시 403 응답")
    void getPostReturnsForbiddenWhenPostIsBlind() throws Exception {
        when(postQueryService.getPost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new ForbiddenException("신고 처리된 게시글"));

        mockMvc.perform(get("/posts/{postNum}", POST_NUM))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("신고 처리된 게시글"));

        verify(postQueryService).getPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("이미지를 포함한 게시글 등록 성공")
    void addPostSuccess() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post.png",
                "image/png",
                new byte[]{1}
        );
        PostRequest request = new PostRequest(
                TITLE,
                CONTENT,
                null,
                null,
                image
        );
        PostResponse response = postResponse();
        when(postCommandService.addPost(SIGN_USER_INFO, request))
                .thenReturn(response);

        mockMvc.perform(multipart("/posts")
                        .file(image)
                        .param("title", TITLE)
                        .param("content", CONTENT))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/posts"))
                .andExpect(jsonPath("$.code").value("게시글 등록 성공"))
                .andExpect(jsonPath("$.data.postNum").value(POST_NUM))
                .andExpect(jsonPath("$.data.blind").value(false))
                .andExpect(jsonPath("$.data.reportCount").doesNotExist())
                .andExpect(jsonPath("$.data.objectKey").doesNotExist());

        verify(postCommandService).addPost(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("임시저장 이미지 objectKey로 게시글 등록 성공")
    void addPostWithTemporaryObjectKeySuccess() throws Exception {
        PostRequest request = new PostRequest(
                TITLE,
                CONTENT,
                15L,
                "posts/temporary.png",
                null
        );
        PostResponse response = postResponse();
        when(postCommandService.addPost(SIGN_USER_INFO, request))
                .thenReturn(response);

        mockMvc.perform(multipart("/posts")
                        .param("title", TITLE)
                        .param("content", CONTENT)
                        .param("temporaryPostId", "15")
                        .param("objectKey", "posts/temporary.png"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postNum").value(POST_NUM))
                .andExpect(jsonPath("$.data.objectKey").doesNotExist());

        verify(postCommandService).addPost(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("temporaryPostId 없이 objectKey를 보내면 400 응답")
    void addPostReturnsBadRequestForObjectKeyWithoutTemporaryPostId()
            throws Exception {
        mockMvc.perform(multipart("/posts")
                        .param("title", TITLE)
                        .param("content", CONTENT)
                        .param("objectKey", "posts/temporary.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 등록 시 objectKey와 파일을 같이 보내면 400 응답")
    void addPostReturnsBadRequestForObjectKeyAndImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post.png",
                "image/png",
                new byte[]{1}
        );

        mockMvc.perform(multipart("/posts")
                        .file(image)
                        .param("title", TITLE)
                        .param("content", CONTENT)
                        .param("objectKey", "posts/temporary.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 등록 사용자가 없으면 404 응답")
    void addPostReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        PostRequest request = postRequest(TITLE, CONTENT);
        when(postCommandService.addPost(SIGN_USER_INFO, request))
                .thenThrow(new NotFoundException("존재하지 않는 유저"));

        performAddPost(TITLE, CONTENT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 유저"));

        verify(postCommandService).addPost(SIGN_USER_INFO, request);
    }

    @Test
    @DisplayName("게시글 등록 시 제목이 공백이면 400 응답")
    void addPostReturnsBadRequestWhenTitleIsBlank() throws Exception {
        performAddPost(" ", CONTENT)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 등록 시 제목이 26자를 초과하면 400 응답")
    void addPostReturnsBadRequestWhenTitleIsTooLong() throws Exception {
        performAddPost("a".repeat(27), CONTENT)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 등록 시 내용이 공백이면 400 응답")
    void addPostReturnsBadRequestWhenContentIsBlank() throws Exception {
        performAddPost(TITLE, " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePostSuccess() throws Exception {
        PostUpdateRequest request = postUpdateRequest(TITLE, CONTENT);
        PostResponse response = postResponse();
        when(postCommandService.updatePost(SIGN_USER_INFO, POST_NUM, request))
                .thenReturn(response);

        performUpdatePost(TITLE, CONTENT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("게시글 수정 성공"))
                .andExpect(jsonPath("$.data.postNum").value(POST_NUM))
                .andExpect(jsonPath("$.data.objectKey").doesNotExist());

        verify(postCommandService).updatePost(SIGN_USER_INFO, POST_NUM, request);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 404 응답")
    void updatePostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        PostUpdateRequest request = postUpdateRequest(TITLE, CONTENT);
        when(postCommandService.updatePost(SIGN_USER_INFO, POST_NUM, request))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        performUpdatePost(TITLE, CONTENT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postCommandService).updatePost(SIGN_USER_INFO, POST_NUM, request);
    }

    @Test
    @DisplayName("다른 사용자의 게시글 수정 시 403 응답")
    void updatePostReturnsForbiddenWhenUserIsNotAuthor() throws Exception {
        PostUpdateRequest request = postUpdateRequest(TITLE, CONTENT);
        when(postCommandService.updatePost(SIGN_USER_INFO, POST_NUM, request))
                .thenThrow(new ForbiddenException("접근 권한 부족"));

        performUpdatePost(TITLE, CONTENT)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근 권한 부족"));

        verify(postCommandService).updatePost(SIGN_USER_INFO, POST_NUM, request);
    }

    @Test
    @DisplayName("게시글 수정 시 제목이 공백이면 400 응답")
    void updatePostReturnsBadRequestWhenTitleIsBlank() throws Exception {
        performUpdatePost(" ", CONTENT)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("게시글 수정 시 objectKey와 파일을 같이 보내면 400 응답")
    void updatePostReturnsBadRequestForObjectKeyAndImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post.png",
                "image/png",
                new byte[]{1}
        );

        performUpdatePost(
                TITLE,
                CONTENT,
                "posts/existing.png",
                image
        ).andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        startsWith("입력데이터가 유효하지 않습니다.")
                ));

        verifyNoInteractions(postCommandService, postQueryService, postInteractionService);
    }

    @Test
    @DisplayName("내 게시글 목록은 요청한 조회 조건으로 불러온다")
    void getMyPostsUsesSignedUserAndRequestedParameters() throws Exception {
        PostPageResponse response = postPageResponse(1, 5);
        when(postQueryService.getMyPosts(SIGN_USER_INFO, 1, 5, "views"))
                .thenReturn(response);

        mockMvc.perform(get("/posts/my")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("내가 쓴 게시글 목록 불러오기 성공"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(postQueryService).getMyPosts(SIGN_USER_INFO, 1, 5, "views");
    }

    @Test
    @DisplayName("게시글 좋아요 추가 성공")
    void likePostSuccess() throws Exception {
        PostLikeResponse response = new PostLikeResponse(1, true);
        when(postInteractionService.likePost(SIGN_USER_INFO, POST_NUM))
                .thenReturn(response);

        mockMvc.perform(post("/posts/{postNum}/like", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("성공"))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(true));

        verify(postInteractionService).likePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 처리 시 404 응답")
    void likePostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        when(postInteractionService.likePost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        mockMvc.perform(post("/posts/{postNum}/like", POST_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postInteractionService).likePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("게시글 좋아요 여부 조회 성공")
    void isLikePostSuccess() throws Exception {
        when(postInteractionService.isLikePost(SIGN_USER_INFO, POST_NUM))
                .thenReturn(true);

        mockMvc.perform(get("/posts/{postNum}/like", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("성공"))
                .andExpect(jsonPath("$.data").value(true));

        verify(postInteractionService).isLikePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 여부 조회 시 404 응답")
    void isLikePostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        when(postInteractionService.isLikePost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        mockMvc.perform(get("/posts/{postNum}/like", POST_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postInteractionService).isLikePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("게시글 신고 성공")
    void reportPostSuccess() throws Exception {
        PostReportResponse response = new PostReportResponse(false);
        when(postInteractionService.reportPost(SIGN_USER_INFO, POST_NUM))
                .thenReturn(response);

        mockMvc.perform(post("/posts/{postNum}/report", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("신고 완료"))
                .andExpect(jsonPath("$.data.blind").value(false))
                .andExpect(jsonPath("$.data.reportCount").doesNotExist());

        verify(postInteractionService).reportPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("본인 게시글 신고 시 400 응답")
    void reportPostReturnsBadRequestWhenReportingOwnPost() throws Exception {
        when(postInteractionService.reportPost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new BadRequestException(
                        "본인이 작성한 글은 신고할 수 없습니다."
                ));

        mockMvc.perform(post("/posts/{postNum}/report", POST_NUM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("본인이 작성한 글은 신고할 수 없습니다."));

        verify(postInteractionService).reportPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("중복 게시글 신고 시 409 응답")
    void reportPostReturnsConflictWhenReportIsDuplicated() throws Exception {
        when(postInteractionService.reportPost(SIGN_USER_INFO, POST_NUM))
                .thenThrow(new DuplicateException("이미 신고한 게시글입니다."));

        mockMvc.perform(post("/posts/{postNum}/report", POST_NUM))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("이미 신고한 게시글입니다."));

        verify(postInteractionService).reportPost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("인기 게시글 목록 조회 성공")
    void getPopularPostsSuccess() throws Exception {
        PostSliceResponse response = new PostSliceResponse(
                List.of(new PopularPostTitleResponse(
                        POST_NUM,
                        "author",
                        null,
                        TITLE,
                        OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
                )),
                0,
                10,
                1,
                false
        );
        when(postQueryService.getTop10PopularPosts()).thenReturn(response);

        mockMvc.perform(get("/posts/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("인기 글 불러오기 성공"))
                .andExpect(jsonPath("$.data.postTitleResponses[0].postNum")
                        .value(POST_NUM))
                .andExpect(jsonPath("$.data.postTitleResponses[0].viewCount")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.postTitleResponses[0].likeCount")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.postTitleResponses[0].reportCount")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.postTitleResponses[0].commentCount")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.postTitleResponses[0].blind")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(postQueryService).getTop10PopularPosts();
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePostSuccess() throws Exception {
        doNothing().when(postCommandService).deletePost(SIGN_USER_INFO, POST_NUM);

        mockMvc.perform(delete("/posts/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("삭제 완료"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(postCommandService).deletePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 404 응답")
    void deletePostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        doThrow(new NotFoundException("존재하지 않는 게시글"))
                .when(postCommandService)
                .deletePost(SIGN_USER_INFO, POST_NUM);

        mockMvc.perform(delete("/posts/{postNum}", POST_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postCommandService).deletePost(SIGN_USER_INFO, POST_NUM);
    }

    @Test
    @DisplayName("권한 없는 게시글 삭제 시 403 응답")
    void deletePostReturnsForbiddenWhenUserHasNoAuthority() throws Exception {
        doThrow(new ForbiddenException("접근 권한 부족"))
                .when(postCommandService)
                .deletePost(SIGN_USER_INFO, POST_NUM);

        mockMvc.perform(delete("/posts/{postNum}", POST_NUM))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근 권한 부족"));

        verify(postCommandService).deletePost(SIGN_USER_INFO, POST_NUM);
    }

    private PostRequest postRequest(String title, String content) {
        return new PostRequest(title, content, null, null, null);
    }

    private PostUpdateRequest postUpdateRequest(String title, String content) {
        return new PostUpdateRequest(
                title,
                content,
                "posts/existing.png",
                null
        );
    }

    private PostPageResponse postPageResponse(int page, int pageSize) {
        return new PostPageResponse(
                List.of(),
                page,
                pageSize,
                0,
                0,
                0
        );
    }

    private PostResponse postResponse() {
        return new PostResponse(
                POST_NUM,
                "author",
                null,
                TITLE,
                CONTENT,
                null,
                0,
                0,
                0,
                false,
                false,
                OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
        );
    }

    private PostDetailResponse postDetailResponse() {
        return new PostDetailResponse(
                POST_NUM,
                "author",
                null,
                TITLE,
                CONTENT,
                null,
                "posts/existing.png",
                0,
                0,
                0,
                false,
                false,
                OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
        );
    }

    private ResultActions performAddPost(
            String title,
            String content
    ) throws Exception {
        return mockMvc.perform(multipart("/posts")
                .param("title", title)
                .param("content", content));
    }

    private ResultActions performUpdatePost(
            String title,
            String content
    ) throws Exception {
        return performUpdatePost(
                title,
                content,
                "posts/existing.png",
                null
        );
    }

    private ResultActions performUpdatePost(
            String title,
            String content,
            String objectKey,
            MockMultipartFile image
    ) throws Exception {
        var requestBuilder = multipart("/posts/{postNum}", POST_NUM)
                .param("title", title)
                .param("content", content)
                .with(request -> {
                    request.setMethod("PATCH");
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
