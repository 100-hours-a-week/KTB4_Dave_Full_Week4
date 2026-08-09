package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.util.ImageUrlBuilder;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminPostPageResponse(
        List<AdminPostTitleResponse> postTitleResponses,
        int page,
        int pageSize,
        int postCount,
        long totalCount,
        int totalPage
) {
    public static AdminPostPageResponse from(
            Page<Post> postPage,
            ImageUrlBuilder imageUrlBuilder
    ) {
        return new AdminPostPageResponse(
                postPage.getContent().stream()
                        .map(post -> AdminPostTitleResponse.from(
                                post,
                                imageUrlBuilder
                        ))
                        .toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getNumberOfElements(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
        );
    }
}
