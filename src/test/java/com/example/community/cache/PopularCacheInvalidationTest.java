package com.example.community.cache;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.comment.event.CommentChangedEvent;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.cache.PopularPostSnapshotStore;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.service.PopularPostSnapshotService;
import com.example.community.user.event.UserDisplayChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PopularCacheInvalidationTest {
    @Mock
    private PopularPostSnapshotStore snapshotStore;
    @Mock
    private PopularPostDetailStore detailStore;
    @Mock
    private PopularCommentStore commentStore;
    @Mock
    private PopularPostSnapshotService snapshotService;

    private PopularCacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        listener = new PopularCacheInvalidationListener(
                snapshotStore,
                detailStore,
                commentStore,
                snapshotService
        );
    }

    @Test
    @DisplayName("게시글 수정은 인기 목록과 해당 본문만 무효화한다")
    void postUpdatedInvalidatesSnapshotAndBody() {
        listener.onPostChanged(new PostChangedEvent.Updated(10L));

        verify(snapshotService).invalidateIfContains(10L);
        verify(detailStore).invalidateBody(10L);
        verifyNoInteractions(commentStore, snapshotStore);
    }

    @Test
    @DisplayName("게시글 삭제와 블라인드는 관련 게시글·댓글 캐시를 제거한다")
    void postRemovedInvalidatesWholePostCache() {
        listener.onPostChanged(new PostChangedEvent.Removed(10L));

        verify(snapshotService).invalidateIfContains(10L);
        verify(detailStore).invalidatePost(10L);
        verify(commentStore).invalidatePost(10L);
    }

    @Test
    @DisplayName("최상위 댓글 등록은 해당 게시글의 첫 페이지 인덱스만 무효화한다")
    void rootCommentCreatedInvalidatesOnlyPageIndex() {
        listener.onCommentChanged(new CommentChangedEvent.Created(10L, null));

        verify(commentStore).invalidateIndex(10L);
        verifyNoInteractions(snapshotService, detailStore, snapshotStore);
    }

    @Test
    @DisplayName("대댓글 삭제는 해당 댓글과 부모 댓글 캐시를 무효화한다")
    void deletedReplyInvalidatesReplyAndParent() {
        listener.onCommentChanged(new CommentChangedEvent.Deleted(30L, 20L));

        verify(commentStore).invalidateComment(30L);
        verify(commentStore).invalidateComment(20L);
        verifyNoInteractions(snapshotService, detailStore, snapshotStore);
    }

    @Test
    @DisplayName("대댓글 등록은 childCount가 변경된 부모 댓글만 무효화한다")
    void replyCreatedInvalidatesParent() {
        listener.onCommentChanged(new CommentChangedEvent.Created(10L, 20L));

        verify(commentStore).invalidateComment(20L);
        verifyNoInteractions(snapshotService, detailStore, snapshotStore);
    }

    @Test
    @DisplayName("프로필 변경은 상태를 제외한 표시 데이터 캐시를 제거한다")
    void userDisplayChangedInvalidatesDisplayCaches() {
        listener.onUserDisplayChanged(new UserDisplayChangedEvent(1L));

        verify(snapshotStore).invalidate();
        verify(detailStore).invalidateDisplayData();
        verify(commentStore).invalidateDisplayData();
        verifyNoInteractions(snapshotService);
    }
}
