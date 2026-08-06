package com.example.community.post.scheduler;

import com.example.community.post.service.PostViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularPostSchedulerTest {
    private static final String DEFAULT_CRON = "10 */5 * * * *";

    @Mock
    private PostViewService postViewService;

    @InjectMocks
    private PopularPostScheduler scheduler;

    @Test
    @DisplayName("인기글 스케줄러는 인기 통계 갱신을 위임한다")
    void delegatesPopularityRefreshToPostViewService() {
        scheduler.refreshPopularPosts();

        verify(postViewService).refreshPopularityStats();
    }

    @Test
    @DisplayName("인기글 스케줄의 기본값은 UTC 기준 매 5분 10초이다")
    void usesEveryFiveMinutesAtTenSecondsUtcByDefault() throws NoSuchMethodException {
        Method refreshMethod = PopularPostScheduler.class.getMethod(
                "refreshPopularPosts"
        );
        Scheduled scheduled = refreshMethod.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron())
                .isEqualTo("${popular-post.scheduler.cron:" + DEFAULT_CRON + "}");
        assertThat(scheduled.zone())
                .isEqualTo("${popular-post.scheduler.zone:UTC}");
    }

    @Test
    @DisplayName("기본 cron은 정해진 UTC 시각에 실행되고 서울 시각으로는 9시간 뒤이다")
    void calculatesTheNextRunAtTheExpectedUtcAndSeoulTime() {
        CronExpression cronExpression = CronExpression.parse(DEFAULT_CRON);
        ZonedDateTime justBeforeRun = ZonedDateTime.of(
                2026, 8, 5, 0, 5, 9, 0, ZoneId.of("UTC")
        );

        ZonedDateTime nextRun = cronExpression.next(justBeforeRun);

        assertThat(nextRun)
                .isEqualTo(ZonedDateTime.of(
                        2026, 8, 5, 0, 5, 10, 0, ZoneId.of("UTC")
                ));
        assertThat(nextRun.withZoneSameInstant(ZoneId.of("Asia/Seoul")))
                .isEqualTo(ZonedDateTime.of(
                        2026, 8, 5, 9, 5, 10, 0, ZoneId.of("Asia/Seoul")
                ));
    }
}
