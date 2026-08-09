package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PopularityWindowPolicy {
    private static final Duration BUCKET_DURATION = Duration.ofMinutes(5);
    private static final Duration THIRTY_MINUTE_WINDOW =
            Duration.ofMinutes(30);
    private static final Duration SIXTY_MINUTE_WINDOW =
            Duration.ofMinutes(60);

    private final PopularPostProperties popularPostProperties;

    public Instant floorToBucket(Instant instant) {
        long bucketSeconds = BUCKET_DURATION.toSeconds();
        long flooredEpochSecond = Math.floorDiv(
                instant.getEpochSecond(),
                bucketSeconds
        ) * bucketSeconds;
        return Instant.ofEpochSecond(flooredEpochSecond);
    }

    public Instant candidateSince(Instant now) {
        return now.minus(popularPostProperties.candidateMaxAge());
    }

    public Instant nextWindowEnd(Instant previousWindowEnd) {
        return previousWindowEnd.plus(BUCKET_DURATION);
    }

    public PopularityWindow windowEndingAt(Instant endAt) {
        return new PopularityWindow(
                endAt.minus(BUCKET_DURATION),
                endAt.minus(THIRTY_MINUTE_WINDOW),
                endAt.minus(SIXTY_MINUTE_WINDOW),
                endAt
        );
    }

    public RollingWindowBoundaries rollingWindowEndingAt(Instant endAt) {
        return new RollingWindowBoundaries(
                endAt.minus(BUCKET_DURATION),
                endAt.minus(THIRTY_MINUTE_WINDOW)
                        .minus(BUCKET_DURATION),
                endAt.minus(SIXTY_MINUTE_WINDOW)
                        .minus(BUCKET_DURATION)
        );
    }

    public record PopularityWindow(
            Instant start5m,
            Instant start30m,
            Instant start60m,
            Instant endAt
    ) {
    }

    public record RollingWindowBoundaries(
            Instant newBucketStartAt,
            Instant expired30MinuteBucketStartAt,
            Instant expired60MinuteBucketStartAt
    ) {
        public List<Instant> bucketStarts() {
            return List.of(
                    newBucketStartAt,
                    expired30MinuteBucketStartAt,
                    expired60MinuteBucketStartAt
            );
        }
    }
}
