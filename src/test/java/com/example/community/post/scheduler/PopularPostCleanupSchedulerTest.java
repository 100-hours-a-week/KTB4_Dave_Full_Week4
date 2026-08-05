package com.example.community.post.scheduler;

import com.example.community.post.service.PostViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularPostCleanupSchedulerTest {
    @Mock
    private PostViewService postViewService;

    @InjectMocks
    private PopularPostCleanupScheduler scheduler;

    @Test
    @DisplayName("일일 정리 스케줄러는 인기글 데이터 정리를 위임한다")
    void delegatesCleanupToPostViewService() {
        scheduler.cleanupExpiredPopularityData();

        verify(postViewService).cleanupExpiredPopularityData();
    }

    @Test
    @DisplayName("일일 정리 스케줄의 기본값은 매일 04시 Asia/Seoul이다")
    void usesDailyFourAmSeoulScheduleByDefault() throws NoSuchMethodException {
        Method cleanupMethod = PopularPostCleanupScheduler.class.getMethod(
                "cleanupExpiredPopularityData"
        );
        Scheduled scheduled = cleanupMethod.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron())
                .isEqualTo("${popular-post.cleanup.cron:0 0 4 * * *}");
        assertThat(scheduled.zone())
                .isEqualTo("${popular-post.cleanup.zone:Asia/Seoul}");
    }
}
