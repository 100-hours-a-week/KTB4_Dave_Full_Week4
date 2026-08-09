package com.example.community.post.service;

import org.springframework.data.domain.Sort;

public final class PostSortPolicy {
    private PostSortPolicy() {
    }

    public static Sort forPosts(String sort) {
        return create(sort, "");
    }

    public static Sort forLikedPosts(String sort) {
        return create(sort, "post.");
    }

    private static Sort create(String sort, String postPrefix) {
        String postNum = postPrefix + "postNum";
        Sort latest = Sort.by(Sort.Direction.DESC, postNum);
        return switch (sort) {
            case "likes" -> Sort.by(
                    Sort.Direction.DESC,
                    postPrefix + "postState.likeCount"
            ).and(latest);
            case "views" -> Sort.by(
                    Sort.Direction.DESC,
                    postPrefix + "postState.viewCount"
            ).and(latest);
            default -> latest;
        };
    }
}
