package com.example.community.post.dto.response;

import com.example.community.post.cache.PopularPostSnapshot;

import java.util.List;

public record PostSliceResponse(
        List<PopularPostTitleResponse> postTitleResponses,
        int page,
        int pageSize,
        int postCount,
        boolean hasNext
) {
    public static PostSliceResponse from(PopularPostSnapshot snapshot){
        return new PostSliceResponse(
                snapshot.orderedPosts(),
                0,
                10,
                snapshot.orderedPosts().size(),
                false
        );
    }
}
