package com.example.community.post.controller;

import com.example.community.post.dto.response.PostEditPageResponse;
import com.example.community.post.dto.response.PostEditResponse;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.dto.response.PostResponse;
import com.example.community.post.service.PostService;
import com.example.community.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/posts")
public class AdminPostController {
    private final PostService postService;

    @GetMapping()
    public ResponseEntity<ApiResponse<PostPageResponse>> getPostByPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        PostPageResponse posts = postService.adminGetPostsByPage(page, size);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable long postNum){
        PostResponse post = postService.adminGetPost(postNum);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 상세 조회 성공", post));
    }

    @GetMapping("/editList/{postNum}")
    public ResponseEntity<ApiResponse<PostEditPageResponse>> getPostEdtByPage(@PathVariable long postNum, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        PostEditPageResponse posts = postService.getPostEditsByPage(postNum, page, size);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 수정 이력 조회 성공", posts));
    }

    @GetMapping("/edit/{editId}")
    public ResponseEntity<ApiResponse<PostEditResponse>> getPostEdtByPage(@PathVariable long editId){
        PostEditResponse posts = postService.getPostEdit(editId);
        return ResponseEntity.ok(new ApiResponse<>("관리자 모드 : 게시글 수정 내용 조회 성공", posts));
    }
}
