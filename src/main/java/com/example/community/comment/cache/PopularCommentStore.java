package com.example.community.comment.cache;

import com.example.community.cache.CacheRegion;
import com.example.community.cache.CacheWeightEstimator;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.function.LongFunction;

import static com.example.community.cache.CachePropertyValidator.requirePositive;

@Component
public class PopularCommentStore {
    private final CacheRegion<Long, PopularCommentFirstPageIndex> indexCache;
    private final CacheRegion<Long, CommentResponse> commentCache;

    @Autowired
    public PopularCommentStore(PopularPostCacheProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    PopularCommentStore(
            PopularPostCacheProperties properties,
            Ticker ticker
    ) {
        validate(properties);
        boolean enabled = properties.isEnabled();
        indexCache = CacheRegion.create(enabled, Caffeine.newBuilder()
                .ticker(ticker)
                .recordStats()
                .expireAfterAccess(properties.getCommentIdleTtl())
                .expireAfterWrite(properties.getCommentMaxTtl())
                .maximumSize(properties.getCommentIndexMaxSize())
                .build());
        commentCache = CacheRegion.create(enabled, Caffeine.newBuilder()
                .ticker(ticker)
                .recordStats()
                .expireAfterAccess(properties.getCommentIdleTtl())
                .expireAfterWrite(properties.getCommentMaxTtl())
                .maximumWeight(properties.getCommentMaxWeightBytes())
                .weigher(CacheWeightEstimator::commentWeight)
                .build());
    }

    public PopularCommentFirstPageIndex getIndex(
            long postNum,
            LongFunction<PopularCommentFirstPageIndex> loader
    ) {
        Objects.requireNonNull(loader);
        return indexCache.get(postNum, loader::apply);
    }

    PopularCommentFirstPageIndex getIndexIfPresent(long postNum) {
        return indexCache.getIfPresent(postNum);
    }

    public CommentResponse getCommentIfPresent(long commentNum) {
        return commentCache.getIfPresent(commentNum);
    }

    public void putComments(Collection<CommentResponse> comments) {
        comments.forEach(comment ->
                commentCache.put(comment.commentNum(), comment)
        );
    }

    public void touch(long postNum) {
        PopularCommentFirstPageIndex index = indexCache.getIfPresent(postNum);
        if (index != null) {
            index.commentNums().forEach(commentCache::getIfPresent);
        }
    }

    public void invalidateComment(long commentNum) {
        commentCache.invalidate(commentNum);
    }

    public void invalidateIndex(long postNum) {
        indexCache.invalidate(postNum);
    }

    public void invalidatePost(long postNum) {
        PopularCommentFirstPageIndex index = indexCache.getIfPresent(postNum);
        if (index != null) {
            commentCache.invalidateAll(index.commentNums());
        }
        indexCache.invalidate(postNum);
    }

    public void invalidateDisplayData() {
        indexCache.invalidateAll();
        commentCache.invalidateAll();
    }

    void runPendingMaintenance() {
        indexCache.runPendingMaintenance();
        commentCache.runPendingMaintenance();
    }

    private static void validate(PopularPostCacheProperties properties) {
        Objects.requireNonNull(properties);
        requirePositive(properties.getCommentIdleTtl(), "commentIdleTtl");
        requirePositive(properties.getCommentMaxTtl(), "commentMaxTtl");
        requirePositive(properties.getCommentMaxWeightBytes(),
                "commentMaxWeightBytes");
        requirePositive(properties.getCommentIndexMaxSize(),
                "commentIndexMaxSize");
    }
}
