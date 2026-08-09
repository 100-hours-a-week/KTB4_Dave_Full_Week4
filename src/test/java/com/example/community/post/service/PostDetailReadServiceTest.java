package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.function.LongFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostDetailReadServiceTest {
    private static final long POST_NUM = 10L;
    private static final PostBodyData BODY =
            new PostBodyData(
                    POST_NUM,
                    "author",
                    null,
                    null,
                    "title",
                    "content",
                    null,
                    null,
                    Instant.parse("2026-08-01T00:00:00Z")
            );
    private static final PostStateData STATE =
            new PostStateData(1, 2, 0, 3);

    @Mock
    private PopularPostSnapshotService snapshotService;
    @Mock
    private PopularPostDetailStore detailStore;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostStateRepository postStateRepository;
    @InjectMocks
    private PostDetailReadService readService;

    @Test
    @DisplayName("인기글은 본문과 상태를 각각 독립된 캐시로 조회한다")
    void popularPostLoadsBodyAndStateThroughIndependentCaches() {
        when(snapshotService.isPopular(POST_NUM)).thenReturn(true);
        when(detailStore.getBody(eq(POST_NUM), any())).thenReturn(BODY);
        when(detailStore.getState(eq(POST_NUM), any())).thenReturn(STATE);

        PostDetailData result = readService.read(POST_NUM);

        assertThat(result.body()).isEqualTo(BODY);
        assertThat(result.state()).isEqualTo(STATE);
        verify(detailStore).getBody(eq(POST_NUM), any());
        verify(detailStore).getState(eq(POST_NUM), any());
        verifyNoInteractions(postRepository, postStateRepository);
    }

    @Test
    @DisplayName("비인기글은 캐시 없이 본문과 상태 projection을 각각 조회한다")
    void nonPopularPostLoadsBothProjectionsWithoutCaching() {
        when(snapshotService.isPopular(POST_NUM)).thenReturn(false);
        when(postRepository.findPostBodyDataByPostNum(POST_NUM))
                .thenReturn(Optional.of(BODY));
        when(postStateRepository.findDataByPostNum(POST_NUM))
                .thenReturn(Optional.of(STATE));

        PostDetailData result = readService.read(POST_NUM);

        assertThat(result).isEqualTo(new PostDetailData(BODY, STATE));
        verify(detailStore, never()).getBody(
                eq(POST_NUM),
                any(LongFunction.class)
        );
        verify(detailStore, never()).getState(
                eq(POST_NUM),
                any(LongFunction.class)
        );
    }

    @Test
    @DisplayName("본문이 없으면 상태를 조회하지 않고 상세 조회에 실패한다")
    void missingBodyStopsBeforeStateLookup() {
        when(snapshotService.isPopular(POST_NUM)).thenReturn(false);
        when(postRepository.findPostBodyDataByPostNum(POST_NUM))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> readService.read(POST_NUM))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
        verifyNoInteractions(postStateRepository);
    }
}
