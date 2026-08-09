package com.example.community.user.controller;

import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.resolver.SignUser;
import com.example.community.resolver.SignUserInfo;
import com.example.community.response.ApiResponse;
import com.example.community.user.service.LikedPostQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class LikedPostController {
    private final LikedPostQueryService likedPostQueryService;

    @GetMapping("/myLike")
    public ResponseEntity<ApiResponse<PostPageResponse>> getMyLikePost(
            @SignUser SignUserInfo signUserInfo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                "좋아요 한 게시글 목록 불러오기 성공",
                likedPostQueryService.getMyLikePosts(
                        signUserInfo,
                        page,
                        size,
                        sort
                )
        ));
    }
}
