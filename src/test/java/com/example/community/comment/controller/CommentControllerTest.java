package com.example.community.comment.controller;

import com.example.community.TestResolverConfig;
import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.service.CommentService;
import com.example.community.configuration.WebConfig;
import com.example.community.filter.JwtFilter;
import com.example.community.filter.RateLimitFilter;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserArgumentResolver;
import com.example.community.resolver.SignUserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommentController.class,
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
class CommentControllerTest {
    private static final SignUserInfo SIGN_USER_INFO =
            TestResolverConfig.SIGN_USER_INFO;
    private static final long POST_NUM = 10L;
    private static final long COMMENT_NUM = 30L;
    private static final long CHILD_COMMENT_NUM = 31L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "commentService")
    private CommentService commentService;

    @Test
    @DisplayName("게시글 댓글 등록 성공")
    void commentToPostSuccess() throws Exception {
        CommentToPostRequest request = new CommentToPostRequest("comment");
        when(commentService.addCommentToPost(
                SIGN_USER_INFO,
                POST_NUM,
                request
        )).thenReturn(new CommentAddResponse(
                1,
                commentResponse(COMMENT_NUM, null, "comment")
        ));

        mockMvc.perform(post("/comments/post/{postNum}", POST_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"comment"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("댓글 등록 성공"))
                .andExpect(jsonPath("$.data.numberOfComments").value(1))
                .andExpect(jsonPath("$.data.comment.content")
                        .value("comment"));

        verify(commentService).addCommentToPost(
                SIGN_USER_INFO,
                POST_NUM,
                request
        );
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 등록 시 404 응답")
    void commentToPostReturnsNotFoundWhenPostDoesNotExist() throws Exception {
        CommentToPostRequest request = new CommentToPostRequest("comment");
        when(commentService.addCommentToPost(
                SIGN_USER_INFO,
                POST_NUM,
                request
        )).thenThrow(new NotFoundException("존재하지 않는 게시글"));

        mockMvc.perform(post("/comments/post/{postNum}", POST_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"comment"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 게시글"));

        verify(commentService).addCommentToPost(
                SIGN_USER_INFO,
                POST_NUM,
                request
        );
    }

    @Test
    @DisplayName("댓글 내용이 공백이면 400 응답")
    void commentToPostReturnsBadRequestWhenContentIsBlank() throws Exception {
        mockMvc.perform(post("/comments/post/{postNum}", POST_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", startsWith(
                        "입력데이터가 유효하지 않습니다."
                )));

        verifyNoInteractions(commentService);
    }

    @Test
    @DisplayName("대댓글 등록 성공")
    void commentToCommentSuccess() throws Exception {
        CommentToCommentRequest request =
                new CommentToCommentRequest("child", COMMENT_NUM);
        when(commentService.addCommentToComment(
                SIGN_USER_INFO,
                POST_NUM,
                request
        )).thenReturn(new CommentAddResponse(
                2,
                commentResponse(
                        CHILD_COMMENT_NUM,
                        COMMENT_NUM,
                        "child"
                )
        ));

        mockMvc.perform(post("/comments/comment/{postNum}", POST_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"child","parentNum":30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("댓글 등록 성공"))
                .andExpect(jsonPath("$.data.numberOfComments").value(2))
                .andExpect(jsonPath("$.data.comment.content").value("child"));

        verify(commentService).addCommentToComment(
                SIGN_USER_INFO,
                POST_NUM,
                request
        );
    }

    @Test
    @DisplayName("존재하지 않는 부모에 대댓글 등록 시 404 응답")
    void commentToCommentReturnsNotFoundWhenParentDoesNotExist()
            throws Exception {
        CommentToCommentRequest request =
                new CommentToCommentRequest("child", COMMENT_NUM);
        when(commentService.addCommentToComment(
                SIGN_USER_INFO,
                POST_NUM,
                request
        )).thenThrow(new NotFoundException("존재하지 않는 댓글"));

        mockMvc.perform(post("/comments/comment/{postNum}", POST_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"child","parentNum":30}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 댓글"));
    }

    @Test
    @DisplayName("게시글 댓글 목록은 기본 조회 조건으로 불러온다")
    void getPostCommentListUsesDefaultParameters() throws Exception {
        CommentPageResponse response = commentPageResponse(0, 10);
        when(commentService.getPostCommentPage(POST_NUM, 0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/comments/list/{postNum}", POST_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("댓글 조회 성공"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.commentResponses[0].commentNum")
                        .value(COMMENT_NUM));

        verify(commentService).getPostCommentPage(POST_NUM, 0, 10);
    }

    @Test
    @DisplayName("대댓글 목록은 요청한 조회 조건으로 불러온다")
    void getChildCommentListUsesRequestedParameters() throws Exception {
        CommentPageResponse response = commentPageResponse(1, 5);
        when(commentService.getChildCommentPage(COMMENT_NUM, 1, 5))
                .thenReturn(response);

        mockMvc.perform(get("/comments/list/child/{commentNum}", COMMENT_NUM)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("자식 댓글 조회 성공"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(commentService).getChildCommentPage(COMMENT_NUM, 1, 5);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateCommentSuccess() throws Exception {
        CommentEditRequest request = new CommentEditRequest("updated");
        CommentResponse response = commentResponse(
                COMMENT_NUM,
                null,
                "updated"
        );
        when(commentService.updateComment(
                SIGN_USER_INFO,
                COMMENT_NUM,
                request
        )).thenReturn(response);

        mockMvc.perform(patch("/comments/{commentNum}", COMMENT_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("댓글 수정 성공"))
                .andExpect(jsonPath("$.data.content").value("updated"));

        verify(commentService).updateComment(
                SIGN_USER_INFO,
                COMMENT_NUM,
                request
        );
    }

    @Test
    @DisplayName("작성자가 아닌 사용자의 댓글 수정 시 403 응답")
    void updateCommentReturnsForbiddenWhenUserIsNotAuthor()
            throws Exception {
        CommentEditRequest request = new CommentEditRequest("updated");
        when(commentService.updateComment(
                SIGN_USER_INFO,
                COMMENT_NUM,
                request
        )).thenThrow(new ForbiddenException("접근 권한 부족"));

        mockMvc.perform(patch("/comments/{commentNum}", COMMENT_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"updated"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근 권한 부족"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 404 응답")
    void updateCommentReturnsNotFoundWhenCommentDoesNotExist()
            throws Exception {
        CommentEditRequest request = new CommentEditRequest("updated");
        when(commentService.updateComment(
                SIGN_USER_INFO,
                COMMENT_NUM,
                request
        )).thenThrow(new NotFoundException("존재하지 않는 댓글"));

        mockMvc.perform(patch("/comments/{commentNum}", COMMENT_NUM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"updated"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 댓글"));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteCommentSuccess() throws Exception {
        doNothing().when(commentService)
                .deleteComment(SIGN_USER_INFO, COMMENT_NUM);

        mockMvc.perform(delete("/comments/{commentNum}", COMMENT_NUM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("댓글 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(commentService).deleteComment(SIGN_USER_INFO, COMMENT_NUM);
    }

    @Test
    @DisplayName("권한 없는 댓글 삭제 시 403 응답")
    void deleteCommentReturnsForbiddenWhenUserHasNoAuthority()
            throws Exception {
        doThrow(new ForbiddenException("접근 권한 부족"))
                .when(commentService)
                .deleteComment(SIGN_USER_INFO, COMMENT_NUM);

        mockMvc.perform(delete("/comments/{commentNum}", COMMENT_NUM))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("접근 권한 부족"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 404 응답")
    void deleteCommentReturnsNotFoundWhenCommentDoesNotExist()
            throws Exception {
        doThrow(new NotFoundException("존재하지 않는 댓글"))
                .when(commentService)
                .deleteComment(SIGN_USER_INFO, COMMENT_NUM);

        mockMvc.perform(delete("/comments/{commentNum}", COMMENT_NUM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("존재하지 않는 댓글"));
    }

    private CommentPageResponse commentPageResponse(int page, int pageSize) {
        return new CommentPageResponse(
                List.of(commentResponse(
                        COMMENT_NUM,
                        null,
                        "comment"
                )),
                page,
                pageSize,
                1,
                1,
                1
        );
    }

    private CommentResponse commentResponse(
            long commentNum,
            Long parentNum,
            String content
    ) {
        return new CommentResponse(
                commentNum,
                POST_NUM,
                parentNum,
                parentNum == null ? 0 : 1,
                "author",
                null,
                content,
                0,
                false,
                false,
                OffsetDateTime.parse("2026-08-05T09:00:00+09:00")
        );
    }
}
