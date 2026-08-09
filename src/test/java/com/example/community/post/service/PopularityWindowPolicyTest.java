package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PopularityWindowPolicyTest {
    private static final Instant NOW =
            Instant.parse("2026-08-02T14:03:20Z");

    private PopularityWindowPolicy windowPolicy;

    @BeforeEach
    void setUp() {
        windowPolicy = new PopularityWindowPolicy(
                new PopularPostProperties(Duration.ofHours(72))
        );
    }

    @Test
    @DisplayName("현재 시각을 직전 5분 버킷 시작 시각으로 내림한다")
    void floorsCurrentTimeToFiveMinuteBucket() {
        assertThat(windowPolicy.floorToBucket(NOW)).isEqualTo(
                Instant.parse("2026-08-02T14:00:00Z")
        );
    }

    @Test
    @DisplayName("후보 시작 시각은 설정된 후보 기간으로 계산한다")
    void calculatesCandidateBoundaryFromConfiguration() {
        assertThat(windowPolicy.candidateSince(NOW)).isEqualTo(
                Instant.parse("2026-07-30T14:03:20Z")
        );
    }

    @Test
    @DisplayName("집계 종료 시각으로 5분 30분 60분 윈도를 계산한다")
    void calculatesPopularityWindow() {
        var window = windowPolicy.windowEndingAt(
                Instant.parse("2026-08-02T14:00:00Z")
        );

        assertThat(window.start5m())
                .isEqualTo(Instant.parse("2026-08-02T13:55:00Z"));
        assertThat(window.start30m())
                .isEqualTo(Instant.parse("2026-08-02T13:30:00Z"));
        assertThat(window.start60m())
                .isEqualTo(Instant.parse("2026-08-02T13:00:00Z"));
    }

    @Test
    @DisplayName("롤링 집계에 들어오고 만료되는 버킷 경계를 계산한다")
    void calculatesRollingWindowBoundaries() {
        var boundaries = windowPolicy.rollingWindowEndingAt(
                Instant.parse("2026-08-02T14:00:00Z")
        );

        assertThat(boundaries.bucketStarts()).containsExactly(
                Instant.parse("2026-08-02T13:55:00Z"),
                Instant.parse("2026-08-02T13:25:00Z"),
                Instant.parse("2026-08-02T12:55:00Z")
        );
    }
}
