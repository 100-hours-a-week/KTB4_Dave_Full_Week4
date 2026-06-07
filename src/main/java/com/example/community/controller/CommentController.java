package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentListResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.domain.token.Token;
import com.example.community.service.CommentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public CommentController(@Qualifier("commentJsonService") CommentService commentService,
                             ObjectMapper objectMapper){
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/post/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToPost(@RequestHeader("Authorization") String token, @PathVariable long postNum , @RequestBody CommentToPostRequest commentToPostRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToPost(access, postNum, commentToPostRequest)));
    }

    @PostMapping("/comment/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToComment(@RequestHeader("Authorization") String token, @PathVariable long postNum, @RequestBody CommentToCommentRequest commentToCommentRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToComment(access, postNum,commentToCommentRequest)));
    }

    @GetMapping("/list/{postNum}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getPostCommentList(@PathVariable long postNum){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", commentService.getPostCommentList(postNum)));
    }

    @PatchMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(@RequestHeader("Authorization") String token, @PathVariable long commentNum, @RequestBody CommentEditRequest  commentEditRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공",commentService.updateComment(access, commentNum, commentEditRequest)));
    }

    @DeleteMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<Object>> updateComment(@RequestHeader("Authorization") String token, @PathVariable long commentNum){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);
        commentService.deleteComment(access, commentNum);
        return ResponseEntity.ok(new ApiResponse<>("댓글 삭제 성공",null));
    }
}
