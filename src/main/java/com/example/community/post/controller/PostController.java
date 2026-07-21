package com.example.community.post.controller;

import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.response.PostLikeResponse;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.dto.response.PostReportResponse;
import com.example.community.post.dto.response.PostResponse;
import com.example.community.post.service.PostService;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<PostPageResponse>> getPostByPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "latest") String sort){
        PostPageResponse posts =postService.getPostsByPage(page, size, sort);
        return ResponseEntity.ok(new ApiResponse<>("게시글 조회 성공", posts));
    }

    @GetMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){
        PostResponse post = postService.getPost(signUserInfo, postNum);
        return ResponseEntity.ok(new ApiResponse<>("게시글 상세 조회 성공", post));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<PostResponse>> addPost(@SignUser SignUserInfo signUserInfo, @ModelAttribute @Valid PostRequest postRequest) throws IOException {

        return ResponseEntity.created(URI.create("/posts"))
                .body(new ApiResponse<>("게시글 등록 성공",postService.addPost(signUserInfo, postRequest)));
    }

    @PatchMapping("/{postNum}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum , @ModelAttribute @Valid PostRequest postRequest) throws IOException {

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postService.updatePost(signUserInfo, postNum, postRequest)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PostPageResponse>> getMyPost(@SignUser SignUserInfo signUserInfo, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,  @RequestParam(defaultValue = "latest") String sort){
        return ResponseEntity.ok(new ApiResponse<>("내가 쓴 게시글 목록 불러오기 성공", postService.getPostsByProfileId(signUserInfo.profileId(), page, size, sort)));
    }

    @PostMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.likePost(signUserInfo, postNum)));
    }

    @GetMapping("/{postNum}/like")
    public ResponseEntity<ApiResponse<Boolean>> isLikePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){

        return  ResponseEntity.ok(new ApiResponse<>("성공", postService.isLikePost(signUserInfo, postNum)));
    }

    @PostMapping("/{postNum}/report")
    public ResponseEntity<ApiResponse<PostReportResponse>> reportPost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){
        return  ResponseEntity.ok(new ApiResponse<>("신고 완료", postService.reportPost(signUserInfo,postNum)));
    }

    @DeleteMapping("/{postNum}")
    public ResponseEntity<ApiResponse<Object>> deletePost(@SignUser SignUserInfo signUserInfo, @PathVariable long postNum){
        postService.deletePost(signUserInfo, postNum);
        return  ResponseEntity.ok(new ApiResponse<>("삭제 완료", null));
    }

}
