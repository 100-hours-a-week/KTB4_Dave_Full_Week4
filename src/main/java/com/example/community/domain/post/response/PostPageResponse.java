package com.example.community.domain.post.response;

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
