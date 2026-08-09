package com.example.community.post.cache;

import com.example.community.cache.CacheRegion;
import com.example.community.cache.CacheWeightEstimator;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.LongFunction;

import static com.example.community.cache.CachePropertyValidator.requirePositive;

@Component
public class PopularPostDetailStore {
    private final CacheRegion<Long, PostBodyData> bodyCache;
    private final CacheRegion<Long, PostStateData> stateCache;

    @Autowired
    public PopularPostDetailStore(PopularPostCacheProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    PopularPostDetailStore(
            PopularPostCacheProperties properties,
            Ticker ticker
    ) {
        validate(properties);
        boolean enabled = properties.isEnabled();
        bodyCache = CacheRegion.create(enabled, Caffeine.newBuilder()
                .ticker(ticker)
                .recordStats()
                .expireAfterAccess(properties.getBodyIdleTtl())
                .expireAfterWrite(properties.getBodyMaxTtl())
                .maximumWeight(properties.getBodyMaxWeightBytes())
                .weigher(CacheWeightEstimator::bodyWeight)
                .build());
        stateCache = CacheRegion.create(enabled, Caffeine.newBuilder()
                .ticker(ticker)
                .recordStats()
                .expireAfterWrite(properties.getStateTtl())
                .maximumSize(properties.getStateMaxSize())
                .build());
    }

    public PostBodyData getBody(
            long postNum,
            LongFunction<PostBodyData> loader
    ) {
        Objects.requireNonNull(loader);
        return bodyCache.get(postNum, loader::apply);
    }

    PostBodyData getBodyIfPresent(long postNum) {
        return bodyCache.getIfPresent(postNum);
    }

    public PostStateData getState(
            long postNum,
            LongFunction<PostStateData> loader
    ) {
        Objects.requireNonNull(loader);
        return stateCache.get(postNum, loader::apply);
    }

    PostStateData getStateIfPresent(long postNum) {
        return stateCache.getIfPresent(postNum);
    }

    public void touch(long postNum) {
        bodyCache.getIfPresent(postNum);
    }

    public void invalidateBody(long postNum) {
        bodyCache.invalidate(postNum);
    }

    public void invalidatePost(long postNum) {
        bodyCache.invalidate(postNum);
        stateCache.invalidate(postNum);
    }

    public void invalidateDisplayData() {
        bodyCache.invalidateAll();
    }

    void runPendingMaintenance() {
        bodyCache.runPendingMaintenance();
        stateCache.runPendingMaintenance();
    }

    private static void validate(PopularPostCacheProperties properties) {
        Objects.requireNonNull(properties);
        requirePositive(properties.getBodyIdleTtl(), "bodyIdleTtl");
        requirePositive(properties.getBodyMaxTtl(), "bodyMaxTtl");
        requirePositive(properties.getStateTtl(), "stateTtl");
        requirePositive(properties.getBodyMaxWeightBytes(),
                "bodyMaxWeightBytes");
        requirePositive(properties.getStateMaxSize(), "stateMaxSize");
    }
}
