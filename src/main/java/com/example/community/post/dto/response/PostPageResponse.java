package com.example.community.post.dto.response;

import java.util.List;

public record PostPageResponse(
        List<PostTitleResponse> postTitleResponses,
        int page,
        int pageSize,
        int postCount,
        long totalCount,
        int totalPage
) {

}
