package com.example.community.comment.controller;

import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.service.CommentService;
import com.example.community.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getPostCommentList(@PathVariable long postNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 댓글 조회 성공", commentService.adminGetPostCommentPage(postNum, page, size)));
    }


    @GetMapping("/child/{commentNum}")
    public ResponseEntity<ApiResponse<CommentPageResponse>> getChildCommentList(@PathVariable long commentNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 댓글 조회 성공", commentService.adminGetChildCommentPage(commentNum, page, size)));
    }

    @GetMapping("/editList/{commentNum}")
    public ResponseEntity<ApiResponse<CommentEditPageResponse>> getCommentEditList(@PathVariable long commentNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 댓글 수정 이력 조회 성공", commentService.getCommentEditsByPage(commentNum, page, size)));
    }
}
