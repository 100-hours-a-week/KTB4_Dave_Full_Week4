package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.PostLikeResponse;
import com.example.community.domain.post.response.PostSliceResponse;
import com.example.community.domain.post.response.PostReportResponse;
import com.example.community.domain.post.response.PostResponse;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.service.post.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(@Qualifier("postJpaService") PostService postService){
        this.postService = postService;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<PostSliceResponse>> getPostByPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int offset){
        PostSliceResponse posts =postService.getPostsByPage(page, offset);
        return ResponseEntity.ok(new ApiResponse<>("게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable long postNum){
        PostResponse post = postService.getPost(postNum);
        return ResponseEntity.ok(new ApiResponse<>("게시글 상세 조회 성공", post));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<PostResponse>> addPost(@SignUser SignUserInfo signUserInfo, @RequestBody @Valid PostRequest postRequest){

        return ResponseEntity.created(URI.create("/posts"))
                .body(new ApiResponse<>("게시글 등록 성공",postService.addPost(signUserInfo, postRequest)));
    }

    @PatchMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum , @RequestBody @Valid PostEditRequest postEditRequest) {

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postService.updatePost(signUserInfo, postNum, postEditRequest)));
    }

    @PostMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.likePost(signUserInfo, postNum)));
    }

    @PatchMapping("/{postNum}/report")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportPost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){
        // 신고를 누가 하는지는 저장안하지만 로그인한 유저만 신고할 수 있도록 토큰 검사

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.reportPost(postNum)));
    }

    @DeleteMapping("/{postNum}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){
        postService.deletePost(signUserInfo, postNum);
        return  ResponseEntity.ok(new ApiResponse<>("성공", null));
    }

}
