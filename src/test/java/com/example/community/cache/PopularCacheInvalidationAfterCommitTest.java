package com.example.community.cache;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.cache.PopularPostSnapshotStore;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.service.PopularPostSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.*;

@SpringBootTest
class PopularCacheInvalidationAfterCommitTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private PopularPostSnapshotStore snapshotStore;
    @MockitoBean
    private PopularPostDetailStore detailStore;
    @MockitoBean
    private PopularCommentStore commentStore;
    @MockitoBean
    private PopularPostSnapshotService snapshotService;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        reset(snapshotStore, detailStore, commentStore, snapshotService);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("업무 변경 이벤트는 트랜잭션 커밋 후 캐시를 무효화한다")
    void invalidatesOnlyAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PostChangedEvent.Updated(10L));
            verifyNoInteractions(
                    snapshotStore,
                    detailStore,
                    commentStore,
                    snapshotService
            );
        });

        verify(snapshotService).invalidateIfContains(10L);
        verify(detailStore).invalidateBody(10L);
        verifyNoInteractions(snapshotStore, commentStore);
    }

    @Test
    @DisplayName("롤백된 트랜잭션의 업무 변경 이벤트는 캐시에 영향을 주지 않는다")
    void doesNotInvalidateAfterRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PostChangedEvent.Updated(10L));
            status.setRollbackOnly();
        });

        verifyNoInteractions(
                snapshotStore,
                detailStore,
                commentStore,
                snapshotService
        );
    }
}
