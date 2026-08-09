package com.example.community.post.controller;

import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.AdminPostPageResponse;
import com.example.community.post.dto.response.AdminPostResponse;
import com.example.community.post.dto.response.PostEditPageResponse;
import com.example.community.post.dto.response.PostEditResponse;
import com.example.community.post.service.AdminPostQueryService;
import com.example.community.resolver.SignUserArgumentResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminPostController.class,
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
class AdminPostControllerTest {

    private static final long POST_NUM = 10L;
    private static final long EDIT_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPostQueryService postQueryService;

    @Test
    @DisplayName("관리자 게시글 목록은 기본 조회 조건으로 불러온다")
    void getPostsUsesDefaultRequestParameters() throws Exception {
        AdminPostPageResponse response = postPageResponse(0, 10);
        when(postQueryService.getPostsByPage(0, 10, "latest"))
                .thenReturn(response);

        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 게시글 조회 성공"))
                .andExpect(jsonPath("$.data.postTitleResponses").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        verify(postQueryService).getPostsByPage(0, 10, "latest");
    }

    @Test
    @DisplayName("관리자 게시글 목록은 요청한 조회 조건으로 불러온다")
    void getPostsUsesRequestedParameters() throws Exception {
        AdminPostPageResponse response = postPageResponse(2, 5);
        when(postQueryService.getPostsByPage(2, 5, "views"))
                .thenReturn(response);

        mockMvc.perform(get("/admin/posts")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(postQueryService).getPostsByPage(2, 5, "views");
    }

    @Test
    @DisplayName("관리자 게시글 상세 조회 성공")
    void getPostSuccess() throws Exception {
        AdminPostResponse response = postResponse();
        when(postQueryService.getPost(POST_NUM)).thenReturn(response);

        mockMvc.perform(get("/admin/posts/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 게시글 상세 조회 성공"))
                .andExpect(jsonPath("$.data.postNum").value(POST_NUM))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.reportCount").value(6))
                .andExpect(jsonPath("$.data.blind").doesNotExist());

        verify(postQueryService).getPost(POST_NUM);
    }

    @Test
    @DisplayName("관리자 상세 조회 시 게시글이 없으면 404 응답")
    void getPostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        when(postQueryService.getPost(POST_NUM))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        mockMvc.perform(get("/admin/posts/{postNum}", POST_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(postQueryService).getPost(POST_NUM);
    }

    @Test
    @DisplayName("게시글 수정 이력 목록은 기본 조회 조건으로 불러온다")
    void getPostEditsUsesDefaultRequestParameters() throws Exception {
        PostEditPageResponse response = postEditPageResponse(0, 10);
        when(postQueryService.getPostEditsByPage(POST_NUM, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/admin/posts/editList/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 게시글 수정 이력 조회 성공"))
                .andExpect(jsonPath("$.data.postEditTitleResponses").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        verify(postQueryService).getPostEditsByPage(POST_NUM, 0, 10);
    }

    @Test
    @DisplayName("게시글 수정 이력 목록은 요청한 페이지로 불러온다")
    void getPostEditsUsesRequestedParameters() throws Exception {
        PostEditPageResponse response = postEditPageResponse(1, 5);
        when(postQueryService.getPostEditsByPage(POST_NUM, 1, 5))
                .thenReturn(response);

        mockMvc.perform(get("/admin/posts/editList/{postNum}", POST_NUM)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(postQueryService).getPostEditsByPage(POST_NUM, 1, 5);
    }

    @Test
    @DisplayName("게시글 수정 이력 상세 조회 성공")
    void getPostEditSuccess() throws Exception {
        PostEditResponse response = new PostEditResponse(
                "old-title",
                "old-content",
                null,
                OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
        );
        when(postQueryService.getPostEdit(EDIT_ID)).thenReturn(response);

        mockMvc.perform(get("/admin/posts/edit/{editId}", EDIT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 게시글 수정 내용 조회 성공"))
                .andExpect(jsonPath("$.data.title").value("old-title"))
                .andExpect(jsonPath("$.data.content").value("old-content"));

        verify(postQueryService).getPostEdit(EDIT_ID);
    }

    @Test
    @DisplayName("수정 이력이 없으면 404 응답")
    void getPostEditReturnsNotFoundWhenEditDoesNotExist() throws Exception {
        when(postQueryService.getPostEdit(EDIT_ID))
                .thenThrow(new NotFoundException("존재하지 않는 수정 이력"));

        mockMvc.perform(get("/admin/posts/edit/{editId}", EDIT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 수정 이력"));

        verify(postQueryService).getPostEdit(EDIT_ID);
    }

    private AdminPostPageResponse postPageResponse(int page, int pageSize) {
        return new AdminPostPageResponse(
                List.of(),
                page,
                pageSize,
                0,
                0,
                0
        );
    }

    private PostEditPageResponse postEditPageResponse(
            int page,
            int pageSize
    ) {
        return new PostEditPageResponse(
                List.of(),
                page,
                pageSize,
                0,
                0,
                0
        );
    }

    private AdminPostResponse postResponse() {
        return new AdminPostResponse(
                POST_NUM,
                "author",
                null,
                "title",
                "content",
                null,
                0,
                0,
                6,
                0,
                false,
                OffsetDateTime.parse("2026-08-05T10:00:00+09:00")
        );
    }
}
