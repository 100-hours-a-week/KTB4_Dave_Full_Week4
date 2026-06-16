package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(@Qualifier("commentJsonService") CommentService commentService){
        this.commentService = commentService;
    }

    @PostMapping("/post/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToPost(@RequestHeader("Authorization") String token, @PathVariable long postNum , @RequestBody @Valid CommentToPostRequest commentToPostRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToPost(token, postNum, commentToPostRequest)));
    }

    @PostMapping("/comment/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToComment(@RequestHeader("Authorization") String token, @PathVariable long postNum, @RequestBody @Valid CommentToCommentRequest commentToCommentRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToComment(token, postNum,commentToCommentRequest)));
    }

    @GetMapping("/list/{postNum}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getPostCommentList(@PathVariable long postNum){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", commentService.getPostCommentList(postNum)));
    }

    @PatchMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(@RequestHeader("Authorization") String token, @PathVariable long commentNum, @RequestBody @Valid CommentEditRequest  commentEditRequest){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공",commentService.updateComment(token, commentNum, commentEditRequest)));
    }

    @DeleteMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<Object>> updateComment(@RequestHeader("Authorization") String token, @PathVariable long commentNum){
        commentService.deleteComment(token, commentNum);
        return ResponseEntity.ok(new ApiResponse<>("댓글 삭제 성공",null));
    }
}
