package com.example.community.post.cache;

import com.example.community.post.dto.response.PopularPostTitleResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record PopularPostSnapshot(
        List<PopularPostTitleResponse> orderedPosts,
        Set<Long> postNums
) {
    public PopularPostSnapshot {
        orderedPosts = List.copyOf(orderedPosts);
        postNums = Set.copyOf(postNums);
    }

    public static PopularPostSnapshot from(
            List<PopularPostTitleResponse> orderedPosts
    ) {
        Set<Long> postNums = orderedPosts.stream()
                .map(PopularPostTitleResponse::postNum)
                .collect(Collectors.toUnmodifiableSet());
        return new PopularPostSnapshot(orderedPosts, postNums);
    }

    public boolean contains(long postNum) {
        return postNums.contains(postNum);
    }
}
