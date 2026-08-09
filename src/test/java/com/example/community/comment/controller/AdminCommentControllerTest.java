package com.example.community.comment.controller;

import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentEditResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.service.AdminCommentQueryService;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.NotFoundException;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminCommentController.class,
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
class AdminCommentControllerTest {
    private static final long POST_NUM = 10L;
    private static final long COMMENT_NUM = 30L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCommentQueryService commentQueryService;

    @Test
    @DisplayName("관리자 댓글 목록은 삭제 댓글 원문과 기본 조회 조건을 반환한다")
    void getPostCommentListSuccess() throws Exception {
        CommentPageResponse response = new CommentPageResponse(
                List.of(deletedCommentResponse()),
                0,
                10,
                1,
                1,
                1
        );
        when(commentQueryService.getPostCommentPage(POST_NUM, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/admin/comments/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 댓글 조회 성공"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.commentResponses[0].content")
                        .value("deleted-content"))
                .andExpect(jsonPath("$.data.commentResponses[0].deleted")
                        .value(true));

        verify(commentQueryService).getPostCommentPage(POST_NUM, 0, 10);
    }

    @Test
    @DisplayName("관리자 대댓글 목록은 요청한 조회 조건으로 불러온다")
    void getChildCommentListSuccess() throws Exception {
        CommentPageResponse response = new CommentPageResponse(
                List.of(),
                1,
                5,
                0,
                0,
                0
        );
        when(commentQueryService.getChildCommentPage(COMMENT_NUM, 1, 5))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/admin/comments/child/{commentNum}",
                        COMMENT_NUM
                ).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 댓글 조회 성공"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(commentQueryService).getChildCommentPage(COMMENT_NUM, 1, 5);
    }

    @Test
    @DisplayName("관리자 대댓글 조회 대상이 없으면 404 응답")
    void getChildCommentListReturnsNotFound() throws Exception {
        when(commentQueryService.getChildCommentPage(COMMENT_NUM, 0, 10))
                .thenThrow(new NotFoundException("존재하지 않는 댓글"));

        mockMvc.perform(get(
                        "/admin/comments/child/{commentNum}",
                        COMMENT_NUM
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 댓글"));
    }

    @Test
    @DisplayName("관리자 댓글 수정 이력 목록 조회 성공")
    void getCommentEditListSuccess() throws Exception {
        CommentEditPageResponse response = new CommentEditPageResponse(
                List.of(new CommentEditResponse(
                        5L,
                        0,
                        "old-content",
                        Instant.parse("2026-08-05T00:00:00Z")
                )),
                0,
                10,
                1,
                1,
                1
        );
        when(commentQueryService.getCommentEditsByPage(COMMENT_NUM, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/admin/comments/editList/{commentNum}",
                        COMMENT_NUM
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("관리자 모드 : 댓글 수정 이력 조회 성공"))
                .andExpect(jsonPath("$.data.commentResponses[0].editId")
                        .value(5L))
                .andExpect(jsonPath("$.data.commentResponses[0].content")
                        .value("old-content"));

        verify(commentQueryService).getCommentEditsByPage(COMMENT_NUM, 0, 10);
    }

    @Test
    @DisplayName("댓글 수정 이력이 없으면 404 응답")
    void getCommentEditListReturnsNotFound() throws Exception {
        when(commentQueryService.getCommentEditsByPage(COMMENT_NUM, 0, 10))
                .thenThrow(new NotFoundException("존재하지 않는 댓글"));

        mockMvc.perform(get(
                        "/admin/comments/editList/{commentNum}",
                        COMMENT_NUM
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 댓글"));
    }

    private CommentResponse deletedCommentResponse() {
        return new CommentResponse(
                COMMENT_NUM,
                POST_NUM,
                null,
                0,
                "author",
                null,
                "deleted-content",
                0,
                false,
                true,
                OffsetDateTime.parse("2026-08-05T09:00:00+09:00")
        );
    }
}
