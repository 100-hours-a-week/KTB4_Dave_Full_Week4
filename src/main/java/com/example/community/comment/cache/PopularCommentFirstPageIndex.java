package com.example.community.comment.cache;

import java.util.List;

public record PopularCommentFirstPageIndex(
        List<Long> commentNums,
        long totalCount
) {
    public static final int PAGE = 0;
    public static final int PAGE_SIZE = 10;

    public PopularCommentFirstPageIndex {
        commentNums = List.copyOf(commentNums);
    }

    public int totalPage() {
        return (int) Math.ceil((double) totalCount / PAGE_SIZE);
    }
}
