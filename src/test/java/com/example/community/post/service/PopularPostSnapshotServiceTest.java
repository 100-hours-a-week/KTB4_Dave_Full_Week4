package com.example.community.post.service;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.cache.PopularPostSnapshot;
import com.example.community.post.cache.PopularPostSnapshotStore;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularPostSnapshotServiceTest {
    @Mock
    private PopularPostSnapshotLoader snapshotLoader;
    @Mock
    private PopularPostSnapshotStore snapshotStore;
    @Mock
    private PopularPostDetailStore detailStore;
    @Mock
    private PopularCommentStore commentStore;
    @InjectMocks
    private PopularPostSnapshotService service;

    @Test
    @DisplayName("스냅샷을 완성한 뒤 캐시에 교체하고 유지·이탈 글을 정리한다")
    void refreshesAndReconcilesSnapshot() {
        PopularPostSnapshot previous = snapshot(1L, 2L);
        PopularPostSnapshot refreshed = snapshot(2L, 3L);
        when(snapshotStore.isEnabled()).thenReturn(true);
        when(snapshotStore.getIfPresent()).thenReturn(previous);
        when(snapshotLoader.load()).thenReturn(refreshed);

        service.refreshSnapshot();

        InOrder order = inOrder(snapshotLoader, snapshotStore);
        order.verify(snapshotLoader).load();
        order.verify(snapshotStore).put(refreshed);
        verify(detailStore).touch(2L);
        verify(commentStore).touch(2L);
        verify(detailStore).invalidatePost(1L);
        verify(commentStore).invalidatePost(1L);
        verify(detailStore, never()).touch(3L);
        verify(commentStore, never()).touch(3L);
    }

    @Test
    @DisplayName("기존 스냅샷이 없으면 새 스냅샷만 저장하고 선조회하지 않는다")
    void storesWithoutReconciliationWhenPreviousSnapshotIsMissing() {
        PopularPostSnapshot refreshed = snapshot(1L, 2L);
        when(snapshotStore.isEnabled()).thenReturn(true);
        when(snapshotStore.getIfPresent()).thenReturn(null);
        when(snapshotLoader.load()).thenReturn(refreshed);

        service.refreshSnapshot();

        verify(snapshotStore).put(refreshed);
        verifyNoInteractions(detailStore, commentStore);
    }

    @Test
    @DisplayName("캐시가 비활성화되면 스케줄 갱신용 DB 조회를 하지 않는다")
    void doesNotRefreshWhenCacheIsDisabled() {
        when(snapshotStore.isEnabled()).thenReturn(false);

        service.refreshSnapshot();

        verifyNoInteractions(snapshotLoader);
        verify(snapshotStore, never()).getIfPresent();
        verify(snapshotStore, never()).put(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("현재 캐시에 포함된 게시글일 때만 목록을 무효화한다")
    void invalidatesOnlyWhenPresentSnapshotContainsPost() {
        when(snapshotStore.getIfPresent()).thenReturn(snapshot(1L, 2L));

        service.invalidateIfContains(2L);

        verify(snapshotStore).invalidate();
    }

    @Test
    @DisplayName("캐시가 없을 때 무효화 검사는 스냅샷을 새로 로딩하지 않는다")
    void invalidationDoesNotLoadMissingSnapshot() {
        when(snapshotStore.getIfPresent()).thenReturn(null);

        service.invalidateIfContains(1L);

        verify(snapshotStore, never()).invalidate();
        verifyNoInteractions(snapshotLoader);
    }

    @Test
    @DisplayName("읽기 membership은 캐시된 스냅샷 포함 여부를 반환한다")
    void checksMembershipFromSnapshot() {
        PopularPostSnapshot snapshot = snapshot(1L, 2L);
        when(snapshotStore.isEnabled()).thenReturn(true);
        when(snapshotStore.get(org.mockito.ArgumentMatchers.any()))
                .thenReturn(snapshot);

        assertThat(service.isPopular(2L)).isTrue();
        assertThat(service.isPopular(3L)).isFalse();
    }

    private PopularPostSnapshot snapshot(long... postNums) {
        List<PopularPostTitleResponse> summaries =
                java.util.Arrays.stream(postNums)
                        .mapToObj(this::summary)
                        .toList();
        return PopularPostSnapshot.from(summaries);
    }

    private PopularPostTitleResponse summary(long postNum) {
        return new PopularPostTitleResponse(
                postNum,
                "author",
                null,
                null,
                "title-" + postNum,
                Instant.EPOCH
        );
    }
}
