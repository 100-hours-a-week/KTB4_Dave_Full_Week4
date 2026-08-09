package com.example.community.temporaryPost.controller;

import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.temporaryPost.dto.request.TemporaryPostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.service.TemporaryPostCommandService;
import com.example.community.temporaryPost.service.TemporaryPostQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/temporaryPost")
@RequiredArgsConstructor
public class TemporaryPostController {
    private final TemporaryPostCommandService temporaryPostCommandService;
    private final TemporaryPostQueryService temporaryPostQueryService;

    @PostMapping()
    public ResponseEntity<ApiResponse<TemporaryKeyResponse>> createTemporaryPost(
            @SignUser SignUserInfo signUserInfo,
            @ModelAttribute @Valid TemporaryPostRequest postRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "임시저장 완료",
                        temporaryPostCommandService.createTemporaryPost(
                                signUserInfo,
                                postRequest
                        )
                ));
    }

    @PutMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> updateTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId, @ModelAttribute @Valid PostUpdateRequest postRequest) {
        return ResponseEntity.ok(new ApiResponse<>("임시저장 완료",temporaryPostCommandService.updateTemporaryPost(signUserInfo, temporaryId, postRequest)));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<TemporaryPostTitleResponse>>> getTemporaryPosts(@SignUser SignUserInfo signUserInfo){
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 목록 불러오기 성공", temporaryPostQueryService.getTemporaryPosts(signUserInfo)));
    }

    @GetMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> getTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId){
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 불러오기 성공", temporaryPostQueryService.getTemporaryPost(signUserInfo, temporaryId)));
    }

    @DeleteMapping("/{temporaryId}")
    public ResponseEntity<ApiResponse<Object>> deleteTemporaryPost(@SignUser SignUserInfo signUserInfo, @PathVariable Long temporaryId){
        temporaryPostCommandService.deleteTemporaryPost(signUserInfo, temporaryId);
        return ResponseEntity.ok(new ApiResponse<>("임시저장 게시글 삭제 성공", null));
    }
}
