package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostView;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostStateRepository;
import com.example.community.post.repository.PostViewRepository;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PostViewRecordingService {
    private final PostRepository postRepository;
    private final PostStateRepository postStateRepository;
    private final PostViewRepository postViewRepository;
    private final UserInfoRepository userInfoRepository;
    private final PopularityViewRecorder popularityViewRecorder;

    @Transactional
    public void record(long profileId, long postNum, Instant writeAt) {
        UserInfo viewer = userInfoRepository.findByProfileId(profileId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));

        boolean shouldCount = postViewRepository
                .findByPost_PostNumAndUserInfo_ProfileId(postNum, profileId)
                .map(PostView::view)
                .orElseGet(() -> createView(postNum, viewer));

        if (!shouldCount) {
            return;
        }
        if (postStateRepository.incrementViewCount(postNum) != 1) {
            throw new NotFoundException("존재하지 않는 게시글");
        }
        popularityViewRecorder.recordView(postNum, writeAt);
    }

    private boolean createView(long postNum, UserInfo viewer) {
        Post postReference = postRepository.getReferenceById(postNum);
        postViewRepository.save(new PostView(postReference, viewer));
        return true;
    }
}
