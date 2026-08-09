package com.example.community.post.controller;

import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.post.dto.response.*;
import com.example.community.post.service.PostCommandService;
import com.example.community.post.service.PostInteractionService;
import com.example.community.post.service.PostQueryService;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostInteractionService postInteractionService;

    @GetMapping()
    public ResponseEntity<ApiResponse<PostPageResponse>> getPostByPage(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size, @RequestParam(defaultValue = "latest") String sort){
        PostPageResponse posts = postQueryService.getPostsByPage(page, size, sort);
        return ResponseEntity.ok(new ApiResponse<>("게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(@SignUser(nullable = true) SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum){
        PostDetailResponse post = postQueryService.getPost(signUserInfo, postNum);
        return ResponseEntity.ok(new ApiResponse<>("게시글 상세 조회 성공", post));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<PostResponse>> addPost(@SignUser SignUserInfo signUserInfo, @ModelAttribute @Valid PostRequest postRequest) {

        return ResponseEntity.created(URI.create("/posts"))
                .body(new ApiResponse<>("게시글 등록 성공", postCommandService.addPost(signUserInfo, postRequest)));
    }

    @PatchMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@SignUser SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum , @ModelAttribute @Valid PostUpdateRequest postRequest) {

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postCommandService.updatePost(signUserInfo, postNum, postRequest)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PostPageResponse>> getMyPost(@SignUser SignUserInfo signUserInfo, @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,  @RequestParam(defaultValue = "latest") String sort){
        return ResponseEntity.ok(new ApiResponse<>("내가 쓴 게시글 목록 불러오기 성공", postQueryService.getMyPosts(signUserInfo, page, size, sort)));
    }

    @PostMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(@SignUser SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum){

        return  ResponseEntity.ok(new ApiResponse<>("성공", postInteractionService.likePost(signUserInfo, postNum)));
    }

    @GetMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<Boolean>> isLikePost(@SignUser SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum){

        return  ResponseEntity.ok(new ApiResponse<>("성공", postInteractionService.isLikePost(signUserInfo, postNum)));
    }

    @PostMapping("/{postNum}/report")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportPost(@SignUser SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum){
        return  ResponseEntity.ok(new ApiResponse<>("신고 완료", postInteractionService.reportPost(signUserInfo,postNum)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PostSliceResponse>> popularPosts(){
        return ResponseEntity.ok(new ApiResponse<>("인기 글 불러오기 성공", postQueryService.getTop10PopularPosts()));
    }

    @DeleteMapping("/{postNum}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@SignUser SignUserInfo signUserInfo, @PathVariable @Min(1) long postNum){
        postCommandService.deletePost(signUserInfo, postNum);
        return  ResponseEntity.ok(new ApiResponse<>("삭제 완료", null));
    }

}
