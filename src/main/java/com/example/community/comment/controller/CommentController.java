package com.example.community.comment.controller;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.service.CommentService;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }

    @PostMapping("/post/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToPost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum , @RequestBody @Valid CommentToPostRequest commentToPostRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToPost(signUserInfo, postNum, commentToPostRequest)));
    }

    @PostMapping("/com/example/community/comment/{postNum}")
    public ResponseEntity<ApiResponse<CommentAddResponse>> commentToComment(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum, @RequestBody @Valid CommentToCommentRequest commentToCommentRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("댓글 등록 성공", commentService.addCommentToComment(signUserInfo, postNum,commentToCommentRequest)));
    }

    @GetMapping("/list/{postNum}")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getPostCommentList(@PathVariable long postNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", commentService.getPostCommentPage(postNum, page, size)));
    }


    @GetMapping("/list/child/{commentNum}")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getChildCommentList(@PathVariable long commentNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", commentService.getChildCommentPage(commentNum, page, size)));
    }


    @PatchMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(@SignUser SignUserInfo signUserInfo, @PathVariable long commentNum, @RequestBody @Valid CommentEditRequest  commentEditRequest){
        return ResponseEntity.ok(new ApiResponse<>("댓글 수정 성공",commentService.updateComment(signUserInfo, commentNum, commentEditRequest)));
    }

    @DeleteMapping("/{commentNum}")
    public ResponseEntity<ApiResponse<Object>> deleteComment(@SignUser SignUserInfo signUserInfo, @PathVariable long commentNum){
        commentService.deleteComment(signUserInfo, commentNum);
        return ResponseEntity.ok(new ApiResponse<>("댓글 삭제 성공",null));
    }
}
