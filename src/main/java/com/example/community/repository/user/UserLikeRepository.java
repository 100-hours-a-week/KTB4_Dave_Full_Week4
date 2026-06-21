package com.example.community.repository.user;

import com.example.community.domain.user.UserLikePostDTO;

import java.util.List;

public interface UserLikeRepository {
    List<UserLikePostDTO> getUserLikePosts(long profileId);
    boolean isUserLikePost(UserLikePostDTO userLikePost);
    void addUserLikePost(UserLikePostDTO userLikePost);
    void deleteUserLikePost(UserLikePostDTO userLikePost);
}
