package com.example.community.comment.service;

import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.post.configuration.PopularPostCacheProperties;
import com.example.community.post.service.PopularPostSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentPageReaderTest {
    private static final long POST_NUM = 10L;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PopularPostSnapshotService snapshotService;

    private PopularCommentStore commentStore;
    private CommentPageReader reader;

    @BeforeEach
    void setUp() {
        commentStore = new PopularCommentStore(
                new PopularPostCacheProperties()
        );
        reader = new CommentPageReader(
                commentRepository,
                snapshotService,
                commentStore
        );
    }

    @Test
    @DisplayName("인기글 댓글 첫 페이지 콜드 미스는 인덱스와 댓글을 한 번만 적재한다")
    void popularFirstPageColdMissLoadsIndexAndItemsOnce() {
        CommentResponse first = comment(1L, "first");
        CommentResponse second = comment(2L, "second");
        when(snapshotService.isPopular(POST_NUM)).thenReturn(true);
        when(commentRepository.findFirstPageResponsesByPostNum(
                eq(POST_NUM),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 10),
                12
        ));

        CommentPageResponse firstRead = reader.read(POST_NUM, 0, 10);
        CommentPageResponse cachedRead = reader.read(POST_NUM, 0, 10);

        assertThat(firstRead).isEqualTo(cachedRead);
        assertThat(firstRead.commentResponses())
                .extracting(response -> response.commentNum())
                .containsExactly(1L, 2L);
        assertThat(firstRead.commentCount()).isEqualTo(2);
        assertThat(firstRead.totalCount()).isEqualTo(12);
        assertThat(firstRead.totalPage()).isEqualTo(2);
        verify(commentRepository, times(1))
                .findFirstPageResponsesByPostNum(
                        eq(POST_NUM),
                        any(Pageable.class)
                );
        verify(commentRepository, never())
                .findResponsesByCommentNumIn(any());
    }

    @Test
    @DisplayName("개별 댓글 부분 미스는 단일 IN 조회로 복구하고 인덱스 순서를 유지한다")
    void partialItemMissUsesOneInQueryAndPreservesIndexOrder() {
        CommentResponse first = comment(1L, "first");
        CommentResponse second = comment(2L, "second");
        CommentResponse refreshedSecond =
                comment(2L, "refreshed-second");
        when(snapshotService.isPopular(POST_NUM)).thenReturn(true);
        when(commentRepository.findFirstPageResponsesByPostNum(
                eq(POST_NUM),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(first, second)));
        when(commentRepository.findResponsesByCommentNumIn(List.of(2L)))
                .thenReturn(List.of(refreshedSecond));

        reader.read(POST_NUM, 0, 10);
        commentStore.invalidateComment(2L);
        CommentPageResponse recovered = reader.read(POST_NUM, 0, 10);

        assertThat(recovered.commentResponses())
                .extracting(response -> response.commentNum())
                .containsExactly(1L, 2L);
        assertThat(recovered.commentResponses().get(1).content())
                .isEqualTo("refreshed-second");
        verify(commentRepository).findResponsesByCommentNumIn(List.of(2L));
        verify(commentRepository, times(1))
                .findFirstPageResponsesByPostNum(
                        eq(POST_NUM),
                        any(Pageable.class)
                );
    }

    @Test
    @DisplayName("비인기글 댓글 첫 페이지는 캐시 없이 기존 페이지 조회를 사용한다")
    void nonPopularFirstPageUsesExistingEntityPageWithoutCaching() {
        when(snapshotService.isPopular(POST_NUM)).thenReturn(false);
        when(commentRepository.findByPost_postNum(
                eq(POST_NUM),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        CommentPageResponse response = reader.read(POST_NUM, 0, 10);

        assertThat(response.commentResponses()).isEmpty();
        verify(commentRepository).findByPost_postNum(
                eq(POST_NUM),
                any(Pageable.class)
        );
        verify(commentRepository, never())
                .findFirstPageResponsesByPostNum(anyLong(), any());
    }

    @Test
    @DisplayName("첫 페이지 기본 크기가 아니면 인기 여부 검사와 캐시를 우회한다")
    void differentPageOrSizeBypassesPopularityCheckAndCache() {
        when(commentRepository.findByPost_postNum(
                eq(POST_NUM),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        reader.read(POST_NUM, 1, 10);
        reader.read(POST_NUM, 0, 20);

        verifyNoInteractions(snapshotService);
        verify(commentRepository, times(2)).findByPost_postNum(
                eq(POST_NUM),
                any(Pageable.class)
        );
    }

    private CommentResponse comment(
            long commentNum,
            String content
    ) {
        return new CommentResponse(
                commentNum,
                POST_NUM,
                null,
                0,
                "author",
                null,
                content,
                0L,
                false,
                false,
                OffsetDateTime.parse("2026-08-08T12:00:00+09:00")
        );
    }
}
