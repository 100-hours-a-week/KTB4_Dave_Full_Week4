package com.example.community.post.controller;

import com.example.community.post.dto.response.AdminPostPageResponse;
import com.example.community.post.dto.response.AdminPostResponse;
import com.example.community.post.dto.response.PostEditPageResponse;
import com.example.community.post.dto.response.PostEditResponse;
import com.example.community.post.service.AdminPostQueryService;
import com.example.community.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/posts")
public class AdminPostController {
    private final AdminPostQueryService postQueryService;

    @GetMapping()
    public ResponseEntity<ApiResponse<AdminPostPageResponse>> getPostByPage(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size, @RequestParam(defaultValue = "latest") String sort){
        AdminPostPageResponse posts = postQueryService.getPostsByPage(page, size, sort);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<AdminPostResponse>> getPost(@PathVariable @Min(1) long postNum){
        AdminPostResponse post = postQueryService.getPost(postNum);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 상세 조회 성공", post));
    }

    @GetMapping("/editList/{postNum}")
    public ResponseEntity<ApiResponse<PostEditPageResponse>> getPostEdtByPage(@PathVariable @Min(1) long postNum, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size){
        PostEditPageResponse posts = postQueryService.getPostEditsByPage(postNum, page, size);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 수정 이력 조회 성공", posts));
    }

    @GetMapping("/edit/{editId}")
    public ResponseEntity<ApiResponse<PostEditResponse>> getPostEdtByPage(@PathVariable long editId){
        PostEditResponse posts = postQueryService.getPostEdit(editId);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 수정 내용 조회 성공", posts));
    }
}
