package com.example.community.post.scheduler;

import com.example.community.post.service.PostViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "popular-post.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PopularPostCleanupScheduler {
    private final PostViewService postViewService;

    @Scheduled(
            cron = "${popular-post.cleanup.cron:0 0 4 * * *}",
            zone = "${popular-post.cleanup.zone:Asia/Seoul}"
    )
    public void cleanupExpiredPopularityData() {
        postViewService.cleanupExpiredPopularityData();
    }
}
