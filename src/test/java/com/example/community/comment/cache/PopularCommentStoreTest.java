package com.example.community.comment.cache;

import com.example.community.cache.MutableTicker;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.post.configuration.PopularPostCacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopularCommentStoreTest {
    private final MutableTicker ticker = new MutableTicker();

    @Test
    @DisplayName("게시글 댓글 제거는 인덱스와 연결된 댓글을 함께 제거한다")
    void invalidatePostRemovesIndexAndComments() {
        PopularCommentStore store = store();
        PopularCommentFirstPageIndex index =
                new PopularCommentFirstPageIndex(List.of(10L, 11L), 2L);
        store.getIndex(1L, ignored -> index);
        store.putComments(List.of(comment(10L), comment(11L)));

        store.invalidatePost(1L);

        assertThat(store.getIndexIfPresent(1L)).isNull();
        assertThat(store.getCommentIfPresent(10L)).isNull();
        assertThat(store.getCommentIfPresent(11L)).isNull();
    }

    @Test
    @DisplayName("표시 데이터 제거는 댓글 인덱스와 댓글을 모두 제거한다")
    void invalidateDisplayDataRemovesAllCommentData() {
        PopularCommentStore store = store();
        store.getIndex(1L, ignored ->
                new PopularCommentFirstPageIndex(List.of(10L), 1L)
        );
        store.putComments(List.of(comment(10L)));

        store.invalidateDisplayData();

        assertThat(store.getIndexIfPresent(1L)).isNull();
        assertThat(store.getCommentIfPresent(10L)).isNull();
    }

    private PopularCommentStore store() {
        return new PopularCommentStore(
                new PopularPostCacheProperties(),
                ticker
        );
    }

    private CommentResponse comment(long commentNum) {
        return new CommentResponse(
                commentNum,
                1L,
                null,
                0,
                "author",
                null,
                "content",
                0L,
                false,
                false,
                OffsetDateTime.parse("2026-08-08T12:00:00+09:00")
        );
    }
}
