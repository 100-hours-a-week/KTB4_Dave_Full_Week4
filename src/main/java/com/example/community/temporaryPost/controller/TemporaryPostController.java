package com.example.community.temporaryPost.controller;

import com.example.community.post.dto.request.PostRequest;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.service.TemporaryPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/temporaryPost")
@RequiredArgsConstructor
public class TemporaryPostController {
    private final TemporaryPostService temporaryPostService;

    @PostMapping()
    public ResponseEntity<ApiResponse<TemporaryKeyResponse>> issueTemporaryId(@SignUser SignUserInfo signUserInfo){
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("임시 저장 ID 발급",temporaryPostService.issueTemporaryId(signUserInfo)));
    }

    @PutMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> updateTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId, @ModelAttribute @Valid PostRequest postRequest) throws IOException {
        System.out.println("controller");
        return ResponseEntity.ok(new ApiResponse<>("임시저장 완료",temporaryPostService.updateTemporaryPost(signUserInfo, temporaryId, postRequest)));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<TemporaryPostTitleResponse>>> getTemporaryPosts(@SignUser SignUserInfo signUserInfo){
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 목록 불러오기 성공", temporaryPostService.getTemporaryPosts(signUserInfo)));
    }

    @GetMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> getTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId){
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 불러오기 성공", temporaryPostService.getTemporaryPost(signUserInfo, temporaryId)));
    }

    @DeleteMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<Object>> deleteTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId){
        temporaryPostService.deleteTemporaryPost(signUserInfo, temporaryId);
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 삭제 성공", null));
    }
}
