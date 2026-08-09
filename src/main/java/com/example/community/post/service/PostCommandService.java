package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.post.dto.response.PostResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostCommandService {
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final UserInfoRepository userInfoRepository;
    private final PostImageResolver postImageResolver;
    private final ImageUrlBuilder imageUrlBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostResponse addPost(
            SignUserInfo signUserInfo,
            PostRequest request
    ) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        String image = postImageResolver.resolveForCreate(signUserInfo, request);
        Post post = new Post(
                userInfo,
                request.title(),
                request.content(),
                image
        );
        postRepository.save(post);
        return PostResponse.from(post, imageUrlBuilder);
    }

    @Transactional
    public PostResponse updatePost(
            SignUserInfo signUserInfo,
            long postNum,
            PostUpdateRequest request
    ) {
        Post post = findOwnedPost(signUserInfo, postNum);
        String image = postImageResolver.resolveForUpdate(post, request);
        recordPostBeforeUpdate(post);
        post.update(request.title(), request.content(), image);
        postRepository.save(post);
        eventPublisher.publishEvent(new PostChangedEvent.Updated(postNum));
        return PostResponse.from(post, imageUrlBuilder);
    }

    @Transactional
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        Post post = findDeletablePost(signUserInfo, postNum);
        post.delete();
        eventPublisher.publishEvent(new PostChangedEvent.Removed(postNum));
    }

    private Post findOwnedPost(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        if (!post.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId())) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    private Post findDeletablePost(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        boolean owner = post.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId());
        if (!owner && signUserInfo.userRole() != UserRole.ADMIN) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
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

    private void recordPostBeforeUpdate(Post post) {
        postEditRepository.save(new PostEditRecord(post));
    }
}
