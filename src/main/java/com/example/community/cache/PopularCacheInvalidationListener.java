package com.example.community.cache;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.comment.event.CommentChangedEvent;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.cache.PopularPostSnapshotStore;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.service.PopularPostSnapshotService;
import com.example.community.user.event.UserDisplayChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PopularCacheInvalidationListener {
    private final PopularPostSnapshotStore snapshotStore;
    private final PopularPostDetailStore detailStore;
    private final PopularCommentStore commentStore;
    private final PopularPostSnapshotService snapshotService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostChanged(PostChangedEvent event) {
        snapshotService.invalidateIfContains(event.postNum());
        switch (event) {
            case PostChangedEvent.Updated ignored ->
                    detailStore.invalidateBody(event.postNum());
            case PostChangedEvent.Removed ignored -> {
                detailStore.invalidatePost(event.postNum());
                commentStore.invalidatePost(event.postNum());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentChanged(CommentChangedEvent event) {
        switch (event) {
            case CommentChangedEvent.Created created ->
                    invalidateCreatedComment(created);
            case CommentChangedEvent.Updated updated ->
                    commentStore.invalidateComment(updated.commentNum());
            case CommentChangedEvent.Deleted deleted -> {
                commentStore.invalidateComment(deleted.commentNum());
                invalidateParentIfPresent(deleted.parentNum());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDisplayChanged(UserDisplayChangedEvent event) {
        snapshotStore.invalidate();
        detailStore.invalidateDisplayData();
        commentStore.invalidateDisplayData();
    }

    private void invalidateCreatedComment(
            CommentChangedEvent.Created event
    ) {
        if (event.parentNum() == null) {
            commentStore.invalidateIndex(event.postNum());
            return;
        }
        commentStore.invalidateComment(event.parentNum());
    }

    private void invalidateParentIfPresent(Long parentNum) {
        if (parentNum != null) {
            commentStore.invalidateComment(parentNum);
        }
    }
}
