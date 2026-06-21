package com.example.community.domain.post.response;

import java.util.List;

public record PostSliceResponse(
        List<PostTitleResponse> postTitleResponses,
        boolean hasNext
) {
}
