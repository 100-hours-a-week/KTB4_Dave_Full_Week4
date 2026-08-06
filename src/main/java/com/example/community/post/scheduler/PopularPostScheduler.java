package com.example.community.post.scheduler;

import com.example.community.post.service.PostViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "popular-post.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PopularPostScheduler {
    private final PostViewService postViewService;

    @Scheduled(
            cron = "${popular-post.scheduler.cron:10 */5 * * * *}",
            zone = "${popular-post.scheduler.zone:UTC}"
    )
    public void refreshPopularPosts() {
        postViewService.refreshPopularityStats();
    }
}
