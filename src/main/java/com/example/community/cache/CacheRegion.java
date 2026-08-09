package com.example.community.cache;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.Objects;
import java.util.function.Function;

public interface CacheRegion<K, V> {
    V get(K key, Function<? super K, ? extends V> loader);

    V getIfPresent(K key);

    void put(K key, V value);

    void invalidate(K key);

    void invalidateAll(Iterable<? extends K> keys);

    void invalidateAll();

    void runPendingMaintenance();

    static <K, V> CacheRegion<K, V> create(
            boolean enabled,
            Cache<K, V> cache
    ) {
        return enabled
                ? new CaffeineCacheRegion<>(cache)
                : new BypassCacheRegion<>();
    }
}

final class CaffeineCacheRegion<K, V> implements CacheRegion<K, V> {
    private final Cache<K, V> cache;

    CaffeineCacheRegion(Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        return cache.get(key, loader);
    }

    @Override
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void invalidate(K key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll(Iterable<? extends K> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void runPendingMaintenance() {
        cache.cleanUp();
    }
}

final class BypassCacheRegion<K, V> implements CacheRegion<K, V> {
    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        return loader.apply(key);
    }

    @Override
    public V getIfPresent(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {
    }

    @Override
    public void invalidate(K key) {
    }

    @Override
    public void invalidateAll(Iterable<? extends K> keys) {
    }

    @Override
    public void invalidateAll() {
    }

    @Override
    public void runPendingMaintenance() {
    }
}
