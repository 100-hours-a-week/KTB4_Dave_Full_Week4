package com.example.community.post.dto.response;

import java.util.List;

public record PostSliceResponse(
        List<PostTitleResponse> postTitleResponses,
        boolean hasNext
) {
}
