package com.example.community.user.repository;

import com.example.community.handler.exception.DuplicateException;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.dto.UserLikePostDTO;
import com.example.community.post.repository.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserLikeJpaAdapterRepository implements UserLikeRepository{
    private final UserLikeJpaRepository userLikeJpaRepository;
    private final UserInfoRepository userInfoRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public Page<UserLikePostDTO> getUserLikePosts(long profileId, Pageable pageable) {
        Page<UserLikePost> userLikePosts = userLikeJpaRepository.findByUserInfo_ProfileId(profileId, pageable);
        List<UserLikePostDTO> userLikePostDTOS = userLikePosts.stream().map(UserLikePostDTO::from).toList();
        return new PageImpl<>(userLikePostDTOS, pageable, userLikePosts.getTotalElements());
    }

    @Override
    public boolean isUserLikePost(UserLikePostDTO userLikePost) {
        return userLikeJpaRepository.existsByUserInfo_ProfileIdAndPost_PostNum(userLikePost.getProfileId(), userLikePost.getPostNum());
    }

    @Override
    public void addUserLikePost(UserLikePostDTO userLikePost) {
        UserInfo userInfo = userInfoRepository.getReferenceById(userLikePost.getProfileId());
        Post post = postJpaRepository.getReferenceById(userLikePost.getPostNum());
        UserLikePost ulp = new UserLikePost(userInfo, post);
        userLikeJpaRepository.save(ulp);
        UserLikePostDTO.from(ulp);
    }

    @Override
    public void deleteUserLikePost(UserLikePostDTO userLikePost) {
        UserLikePost ulp = userLikeJpaRepository
                .findByUserInfo_ProfileIdAndPost_PostNum(userLikePost.getProfileId(), userLikePost.getPostNum())
                .orElseThrow(() -> new DuplicateException("좋아요가 이미 취소된 상태"));

        userLikeJpaRepository.delete(ulp);
    }
}
