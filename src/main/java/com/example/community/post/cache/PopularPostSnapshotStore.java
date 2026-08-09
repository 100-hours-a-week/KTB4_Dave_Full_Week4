package com.example.community.post.cache;

import com.example.community.cache.CacheRegion;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

import static com.example.community.cache.CachePropertyValidator.requirePositive;

@Component
public class PopularPostSnapshotStore {
    private static final String SNAPSHOT_KEY = "current";

    private final boolean enabled;
    private final CacheRegion<String, PopularPostSnapshot> snapshotCache;

    @Autowired
    public PopularPostSnapshotStore(PopularPostCacheProperties properties) {
        this(properties, Ticker.systemTicker());
    }

    PopularPostSnapshotStore(
            PopularPostCacheProperties properties,
            Ticker ticker
    ) {
        validate(properties);
        enabled = properties.isEnabled();
        snapshotCache = CacheRegion.create(enabled, Caffeine.newBuilder()
                .ticker(ticker)
                .recordStats()
                .expireAfterWrite(properties.getListTtl())
                .maximumSize(1)
                .build());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PopularPostSnapshot get(Supplier<PopularPostSnapshot> loader) {
        Objects.requireNonNull(loader);
        return snapshotCache.get(SNAPSHOT_KEY, ignored -> loader.get());
    }

    public PopularPostSnapshot getIfPresent() {
        return snapshotCache.getIfPresent(SNAPSHOT_KEY);
    }

    public void put(PopularPostSnapshot snapshot) {
        snapshotCache.put(SNAPSHOT_KEY, Objects.requireNonNull(snapshot));
    }

    public void invalidate() {
        snapshotCache.invalidate(SNAPSHOT_KEY);
    }

    void runPendingMaintenance() {
        snapshotCache.runPendingMaintenance();
    }

    private static void validate(PopularPostCacheProperties properties) {
        Objects.requireNonNull(properties);
        requirePositive(properties.getListTtl(), "listTtl");
    }
}
