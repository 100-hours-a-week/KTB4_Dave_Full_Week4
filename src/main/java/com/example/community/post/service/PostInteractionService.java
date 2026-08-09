package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.PostLikeResponse;
import com.example.community.post.dto.response.PostReportResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostReport;
import com.example.community.post.entity.PostState;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.repository.PostReportRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostInteractionService {
    private final PostRepository postRepository;
    private final PostReportRepository postReportRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeRepository userLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostLikeResponse likePost(
            SignUserInfo signUserInfo,
            long postNum
    ) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        Optional<UserLikePost> existingLike = userLikeRepository
                .findByUserInfo_ProfileIdAndPost_PostNum(
                        signUserInfo.profileId(),
                        postNum
                );
        existingLike.ifPresentOrElse(
                this::unlike,
                () -> like(userInfo, post)
        );
        return new PostLikeResponse(
                post.getPostState().getLikeCount(),
                existingLike.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public boolean isLikePost(
            SignUserInfo signUserInfo,
            long postNum
    ) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        return userLikeRepository
                .existsByUserInfo_ProfileIdAndPost_PostNum(
                        userInfo.getProfileId(),
                        post.getPostNum()
                );
    }

    @Transactional
    public PostReportResponse reportPost(
            SignUserInfo signUserInfo,
            long postNum
    ) {
        Post post = findPost(postNum);
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        validateReport(signUserInfo, post, userInfo);
        postReportRepository.save(new PostReport(post, userInfo));
        publishRemovalWhenBlind(postNum, post);
        return new PostReportResponse(post.isBlind());
    }

    private void validateReport(
            SignUserInfo signUserInfo,
            Post post,
            UserInfo userInfo
    ) {
        if (post.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId())) {
            throw new BadRequestException(
                    "본인이 작성한 글은 신고할 수 없습니다."
            );
        }
        boolean alreadyReported = postReportRepository
                .existsByPost_PostNumAndUserInfo_ProfileId(
                        post.getPostNum(),
                        userInfo.getProfileId()
                );
        if (alreadyReported) {
            throw new DuplicateException("이미 신고한 게시글입니다.");
        }
    }

    private void publishRemovalWhenBlind(long postNum, Post post) {
        if (post.getPostState().getReportCount()
                == PostState.BLIND_REPORT_THRESHOLD) {
            eventPublisher.publishEvent(new PostChangedEvent.Removed(postNum));
        }
    }

    private Post findPost(long postNum) {
        return postRepository.findByPostNum(postNum)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 게시글")
                );
    }

    private UserInfo findUserInfo(long profileId) {
        return userInfoRepository.findByProfileId(profileId)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
    }

    private void unlike(UserLikePost userLikePost) {
        userLikePost.getPost().unlike();
        userLikeRepository.delete(userLikePost);
    }

    private void like(UserInfo userInfo, Post post) {
        userLikeRepository.save(new UserLikePost(userInfo, post));
    }
}
