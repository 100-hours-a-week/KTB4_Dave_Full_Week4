package com.example.community.cache;

import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.post.dto.query.PostBodyData;

public final class CacheWeightEstimator {
    private static final int BODY_FIXED_BYTES = 256;
    private static final int COMMENT_FIXED_BYTES = 256;
    private static final int STRING_FIXED_BYTES = 24;

    private CacheWeightEstimator() {
    }

    public static int bodyWeight(Long ignoredKey, PostBodyData value) {
        return clamp(BODY_FIXED_BYTES
                + stringWeight(value.nickname())
                + stringWeight(value.profileImage())
                + stringWeight(value.title())
                + stringWeight(value.content())
                + stringWeight(value.imageObjectKey()));
    }

    public static int commentWeight(Long ignoredKey, CommentResponse value) {
        return clamp(COMMENT_FIXED_BYTES
                + stringWeight(value.nickname())
                + stringWeight(value.profileImage())
                + stringWeight(value.content()));
    }

    private static long stringWeight(String value) {
        return value == null
                ? 0L
                : STRING_FIXED_BYTES + (long) value.length() * 2L;
    }

    private static int clamp(long weight) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, weight));
    }
}
