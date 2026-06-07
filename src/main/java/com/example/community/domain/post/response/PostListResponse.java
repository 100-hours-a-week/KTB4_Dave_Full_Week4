package com.example.community.domain.post.response;

import java.util.List;

public record PostListResponse(
        List<PostTitleResponse> postTitleResponses,
        boolean hasNext
) {
}
