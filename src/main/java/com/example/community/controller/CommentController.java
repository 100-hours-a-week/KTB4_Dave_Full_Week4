package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
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
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToPost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum , @RequestBody @Valid CommentToPostRequest commentToPostRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToPost(signUserInfo, postNum, commentToPostRequest)));
    }

    @PostMapping("/comment/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToComment(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum, @RequestBody @Valid CommentToCommentRequest commentToCommentRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToComment(signUserInfo, postNum,commentToCommentRequest)));
    }

    @GetMapping("/list/{postNum}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getPostCommentList(@PathVariable long postNum){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", commentService.getPostCommentList(postNum)));
    }

    @PatchMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(@SignUser SignUserInfo signUserInfo, @PathVariable long commentNum, @RequestBody @Valid CommentEditRequest  commentEditRequest){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공",commentService.updateComment(signUserInfo, commentNum, commentEditRequest)));
    }

    @DeleteMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<Object>> updateComment(@SignUser SignUserInfo signUserInfo, @PathVariable long commentNum){
        commentService.deleteComment(signUserInfo, commentNum);
        return ResponseEntity.ok(new ApiResponse<>("댓글 삭제 성공",null));
    }
}
