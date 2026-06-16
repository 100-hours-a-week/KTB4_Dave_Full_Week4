package com.example.community.controller;

import com.example.community.domain.ApiResponse;
import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.PostLikeResponse;
import com.example.community.domain.post.response.PostListResponse;
import com.example.community.domain.post.response.PostReportResponse;
import com.example.community.domain.post.response.PostResponse;
import com.example.community.domain.token.Token;
import com.example.community.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    ObjectMapper objectMapper;

    public PostController(@Qualifier("postJsonService") PostService postService, ObjectMapper objectMapper){
        this.postService = postService;
        this.objectMapper = objectMapper;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<PostListResponse>> getPostByPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int offset){
        PostListResponse posts =postService.getPostsByPage(page, offset);
        return ResponseEntity.ok(new ApiResponse<>("게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable long postNum){
        PostResponse post = postService.getPost(postNum);
        return ResponseEntity.ok(new ApiResponse<>("게시글 상세 조회 성공", post));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<PostResponse>> addPost(@RequestHeader("Authorization") String token , @RequestBody @Valid PostRequest postRequest){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.created(URI.create("/posts"))
                .body(new ApiResponse<>("게시글 등록 성공",postService.addPost(access, postRequest)));
    }

    @PatchMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@RequestHeader("Authorization") String token, @PathVariable long postNum , @RequestBody @Valid PostEditRequest postEditRequest) {
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postService.updatePost(access, postNum, postEditRequest)));
    }

    @PostMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(@RequestHeader("Authorization") String token, @PathVariable long postNum){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.likePost(access, postNum)));
    }

    @PatchMapping("/{postNum}/report")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportPost(@RequestHeader("Authorization") String token, @PathVariable long postNum){
        // 신고를 누가 하는지는 저장안하지만 로그인한 유저만 신고할 수 있도록 토큰 검사

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.reportPost(postNum)));
    }

    @DeleteMapping("/{postNum}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@RequestHeader("Authorization") String token, @PathVariable long postNum){
        String decoded = URLDecoder.decode(token, StandardCharsets.UTF_8);
        Token access = objectMapper.readValue(decoded, Token.class);
        postService.deletePost(access, postNum);
        return  ResponseEntity.ok(new ApiResponse<>("성공", null));
    }

}
