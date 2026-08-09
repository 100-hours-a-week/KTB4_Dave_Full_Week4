package com.example.community.post.service;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.cache.PopularPostSnapshot;
import com.example.community.post.cache.PopularPostSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PopularPostSnapshotService {
    private final PopularPostSnapshotLoader snapshotLoader;
    private final PopularPostSnapshotStore snapshotStore;
    private final PopularPostDetailStore detailStore;
    private final PopularCommentStore commentStore;

    public PopularPostSnapshot getSnapshot() {
        return snapshotStore.get(snapshotLoader::load);
    }

    public boolean isPopular(long postNum) {
        return snapshotStore.isEnabled() && getSnapshot().contains(postNum);
    }

    public void refreshSnapshot() {
        if (!snapshotStore.isEnabled()) {
            return;
        }

        PopularPostSnapshot previous = snapshotStore.getIfPresent();
        PopularPostSnapshot refreshed = snapshotLoader.load();
        snapshotStore.put(refreshed);
        reconcile(previous, refreshed);
    }

    public void invalidateIfContains(long postNum) {
        PopularPostSnapshot snapshot = snapshotStore.getIfPresent();
        if (snapshot != null && snapshot.contains(postNum)) {
            snapshotStore.invalidate();
        }
    }

    private void reconcile(
            PopularPostSnapshot previous,
            PopularPostSnapshot refreshed
    ) {
        if (previous == null) {
            return;
        }

        Set<Long> retained = new HashSet<>(previous.postNums());
        retained.retainAll(refreshed.postNums());
        retained.forEach(this::touchRetainedPost);

        Set<Long> removed = new HashSet<>(previous.postNums());
        removed.removeAll(refreshed.postNums());
        removed.forEach(this::invalidateRemovedPost);
    }

    private void touchRetainedPost(long postNum) {
        detailStore.touch(postNum);
        commentStore.touch(postNum);
    }

    private void invalidateRemovedPost(long postNum) {
        detailStore.invalidatePost(postNum);
        commentStore.invalidatePost(postNum);
    }
}
