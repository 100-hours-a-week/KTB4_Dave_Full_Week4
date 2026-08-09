package com.example.community.post.cache;

import com.example.community.cache.MutableTicker;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PopularPostDetailStoreTest {
    private final MutableTicker ticker = new MutableTicker();

    @Test
    @DisplayName("본문 touch는 idle TTL만 연장하고 30분 write TTL은 연장하지 않는다")
    void bodyTouchDoesNotExtendMaximumTtl() {
        PopularPostDetailStore store = store(properties(true));
        AtomicInteger loads = new AtomicInteger();

        assertThat(store.getBody(1L, ignored -> body(loads.incrementAndGet())))
                .isEqualTo(body(1));
        ticker.advance(Duration.ofMinutes(9));
        assertThat(store.getBody(1L, ignored -> body(loads.incrementAndGet())))
                .isEqualTo(body(1));
        ticker.advance(Duration.ofMinutes(9));
        assertThat(store.getBody(1L, ignored -> body(loads.incrementAndGet())))
                .isEqualTo(body(1));
        ticker.advance(Duration.ofMinutes(9));
        assertThat(store.getBody(1L, ignored -> body(loads.incrementAndGet())))
                .isEqualTo(body(1));
        ticker.advance(Duration.ofMinutes(4));
        assertThat(store.getBody(1L, ignored -> body(loads.incrementAndGet())))
                .isEqualTo(body(2));
        assertThat(loads).hasValue(2);
    }

    @Test
    @DisplayName("상태 접근은 1분 write TTL을 연장하지 않는다")
    void stateAccessDoesNotExtendTtl() {
        PopularPostDetailStore store = store(properties(true));
        AtomicInteger loads = new AtomicInteger();

        assertThat(store.getState(1L, ignored -> state(loads.incrementAndGet())))
                .isEqualTo(state(1));
        ticker.advance(Duration.ofSeconds(30));
        assertThat(store.getState(1L, ignored -> state(loads.incrementAndGet())))
                .isEqualTo(state(1));
        ticker.advance(Duration.ofSeconds(31));
        assertThat(store.getState(1L, ignored -> state(loads.incrementAndGet())))
                .isEqualTo(state(2));
        assertThat(loads).hasValue(2);
    }

    @Test
    @DisplayName("본문 weight가 전체 예산보다 크면 캐시에 유지하지 않는다")
    void oversizedBodyIsNotRetained() {
        PopularPostCacheProperties properties = properties(true);
        properties.setBodyMaxWeightBytes(1_000L);
        PopularPostDetailStore store = store(properties);
        PostBodyData oversized = new PostBodyData(
                1L, "author", null, null, "title",
                "x".repeat(1_000), null, null, Instant.EPOCH
        );

        assertThat(store.getBody(1L, ignored -> oversized))
                .isEqualTo(oversized);
        store.runPendingMaintenance();
        assertThat(store.getBodyIfPresent(1L)).isNull();
    }

    @Test
    @DisplayName("게시글 제거는 본문과 상태를 함께 제거한다")
    void invalidatePostRemovesBodyAndState() {
        PopularPostDetailStore store = store(properties(true));
        store.getBody(1L, ignored -> body(1));
        store.getState(1L, ignored -> state(1));

        store.invalidatePost(1L);

        assertThat(store.getBodyIfPresent(1L)).isNull();
        assertThat(store.getStateIfPresent(1L)).isNull();
    }

    @Test
    @DisplayName("표시 데이터 제거는 상태 캐시를 유지한다")
    void invalidateDisplayDataKeepsState() {
        PopularPostDetailStore store = store(properties(true));
        PostStateData state = state(1);
        store.getBody(1L, ignored -> body(1));
        store.getState(1L, ignored -> state);

        store.invalidateDisplayData();

        assertThat(store.getBodyIfPresent(1L)).isNull();
        assertThat(store.getStateIfPresent(1L)).isEqualTo(state);
    }

    @Test
    @DisplayName("캐시가 비활성화되면 본문 로더를 매번 실행한다")
    void disabledCacheAlwaysUsesLoader() {
        PopularPostDetailStore store = store(properties(false));
        AtomicInteger loads = new AtomicInteger();

        store.getBody(1L, ignored -> body(loads.incrementAndGet()));
        store.getBody(1L, ignored -> body(loads.incrementAndGet()));

        assertThat(loads).hasValue(2);
        assertThat(store.getBodyIfPresent(1L)).isNull();
    }

    private PopularPostDetailStore store(
            PopularPostCacheProperties properties
    ) {
        return new PopularPostDetailStore(properties, ticker);
    }

    private PopularPostCacheProperties properties(boolean enabled) {
        PopularPostCacheProperties properties =
                new PopularPostCacheProperties();
        properties.setEnabled(enabled);
        return properties;
    }

    private PostBodyData body(int marker) {
        return new PostBodyData(
                1L, "author", null, null, "title-" + marker,
                "content", null, null, Instant.EPOCH
        );
    }

    private PostStateData state(int marker) {
        return new PostStateData(marker, 0, 0, 0);
    }
}
