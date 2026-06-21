package com.example.community.repository.user;

import com.example.community.domain.exception.DuplicateException;
import com.example.community.domain.post.Post;
import com.example.community.domain.user.UserInfo;
import com.example.community.domain.user.UserLikePost;
import com.example.community.domain.user.UserLikePostDTO;
import com.example.community.repository.post.PostJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserLikeJpaAdapterRepository implements UserLikeRepository{
    private final UserLikeJpaRepository userLikeJpaRepository;
    private final UserInfoJpaRepository userInfoJpaRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public List<UserLikePostDTO> getUserLikePosts(long profileId) {
        return userLikeJpaRepository.findByUserInfo_ProfileId(profileId)
                .stream().map(UserLikePostDTO::from).toList();
    }

    @Override
    public boolean isUserLikePost(UserLikePostDTO userLikePost) {
        return userLikeJpaRepository.existsByUserInfo_ProfileIdAndPost_PostNum(userLikePost.getProfileId(), userLikePost.getPostNum());
    }

    @Override
    public void addUserLikePost(UserLikePostDTO userLikePost) {
        UserInfo userInfo = userInfoJpaRepository.getReferenceById(userLikePost.getProfileId());
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
