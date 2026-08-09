package com.example.community.post.cache;

import com.example.community.cache.MutableTicker;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PopularPostSnapshotStoreTest {
    private final MutableTicker ticker = new MutableTicker();

    @Test
    @DisplayName("목록 스냅샷은 15분 write TTL 동안 한 번만 로딩한다")
    void snapshotExpiresAfterWrite() {
        PopularPostSnapshotStore store = store(properties(true));
        AtomicInteger loads = new AtomicInteger();

        assertThat(store.get(() -> snapshot(loads.incrementAndGet())))
                .isEqualTo(snapshot(1));
        ticker.advance(Duration.ofMinutes(14));
        assertThat(store.get(() -> snapshot(loads.incrementAndGet())))
                .isEqualTo(snapshot(1));
        ticker.advance(Duration.ofMinutes(2));
        assertThat(store.get(() -> snapshot(loads.incrementAndGet())))
                .isEqualTo(snapshot(2));
        assertThat(loads).hasValue(2);
    }

    @Test
    @DisplayName("캐시가 비활성화되면 스냅샷을 저장하지 않는다")
    void disabledCacheAlwaysUsesLoader() {
        PopularPostSnapshotStore store = store(properties(false));
        AtomicInteger loads = new AtomicInteger();

        store.get(() -> snapshot(loads.incrementAndGet()));
        store.get(() -> snapshot(loads.incrementAndGet()));

        assertThat(loads).hasValue(2);
        assertThat(store.getIfPresent()).isNull();
    }

    private PopularPostSnapshotStore store(
            PopularPostCacheProperties properties
    ) {
        return new PopularPostSnapshotStore(properties, ticker);
    }

    private PopularPostCacheProperties properties(boolean enabled) {
        PopularPostCacheProperties properties =
                new PopularPostCacheProperties();
        properties.setEnabled(enabled);
        return properties;
    }

    private PopularPostSnapshot snapshot(int marker) {
        PopularPostTitleResponse summary = new PopularPostTitleResponse(
                marker,
                "author",
                null,
                null,
                "title",
                Instant.EPOCH
        );
        return PopularPostSnapshot.from(List.of(summary));
    }
}
