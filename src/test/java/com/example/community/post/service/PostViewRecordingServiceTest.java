package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostView;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostStateRepository;
import com.example.community.post.repository.PostViewRepository;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostViewRecordingServiceTest {
    private static final long POST_NUM = 10L;
    private static final long PROFILE_ID = 1L;
    private static final Instant WRITE_AT =
            Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostStateRepository postStateRepository;
    @Mock
    private PostViewRepository postViewRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private PopularityViewRecorder popularityViewRecorder;

    private PostViewRecordingService recordingService;
    private UserInfo viewer;

    @BeforeEach
    void setUp() {
        recordingService = new PostViewRecordingService(
                postRepository,
                postStateRepository,
                postViewRepository,
                userInfoRepository,
                popularityViewRecorder
        );
        viewer = new UserInfo(
                new SignInfo("viewer@example.com", "password"),
                "viewer",
                null
        );
        viewer.setProfileId(PROFILE_ID);
    }

    @Test
    @DisplayName("신규 조회는 이력을 생성하고 상태 테이블의 조회수를 증가시킨다")
    void newViewCreatesHistoryAndIncrementsStateDirectly() {
        Post postReference = new Post();
        when(userInfoRepository.findByProfileId(PROFILE_ID))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                POST_NUM,
                PROFILE_ID
        )).thenReturn(Optional.empty());
        when(postRepository.getReferenceById(POST_NUM))
                .thenReturn(postReference);
        when(postStateRepository.incrementViewCount(POST_NUM)).thenReturn(1);

        recordingService.record(PROFILE_ID, POST_NUM, WRITE_AT);

        verify(postViewRepository).save(any(PostView.class));
        verify(postStateRepository).incrementViewCount(POST_NUM);
        verify(popularityViewRecorder).recordView(POST_NUM, WRITE_AT);
    }

    @Test
    @DisplayName("24시간 이내 재조회는 조회수와 인기 조회 기록을 변경하지 않는다")
    void recentViewDoesNotIncrementOrRecordPopularity() {
        PostView recentView = org.mockito.Mockito.mock(PostView.class);
        when(userInfoRepository.findByProfileId(PROFILE_ID))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                POST_NUM,
                PROFILE_ID
        )).thenReturn(Optional.of(recentView));
        when(recentView.view()).thenReturn(false);

        recordingService.record(PROFILE_ID, POST_NUM, WRITE_AT);

        verify(postViewRepository, never()).save(any());
        verifyNoInteractions(postStateRepository, popularityViewRecorder);
    }

    @Test
    @DisplayName("24시간이 지난 재조회는 조회수와 인기 조회 기록을 갱신한다")
    void expiredViewIncrementsStateAndRecordsPopularity() {
        PostView expiredView = org.mockito.Mockito.mock(PostView.class);
        when(userInfoRepository.findByProfileId(PROFILE_ID))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                POST_NUM,
                PROFILE_ID
        )).thenReturn(Optional.of(expiredView));
        when(expiredView.view()).thenReturn(true);
        when(postStateRepository.incrementViewCount(POST_NUM)).thenReturn(1);

        recordingService.record(PROFILE_ID, POST_NUM, WRITE_AT);

        verify(postStateRepository).incrementViewCount(POST_NUM);
        verify(popularityViewRecorder).recordView(POST_NUM, WRITE_AT);
    }

    @Test
    @DisplayName("조회 사용자가 없으면 조회 이력을 확인하기 전에 실패한다")
    void missingViewerFailsBeforeViewLookup() {
        when(userInfoRepository.findByProfileId(PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordingService.record(
                PROFILE_ID,
                POST_NUM,
                WRITE_AT
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
        verifyNoInteractions(postViewRepository, postStateRepository);
    }
}
