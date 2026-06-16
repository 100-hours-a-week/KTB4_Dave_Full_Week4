package com.example.community.repository;

import com.example.community.domain.user.UserLikePostDTO;

import java.util.List;

public interface UserLikeRepository {
    List<UserLikePostDTO> getUserLikePosts(long userNum);
    boolean isUserLikePost(UserLikePostDTO userLikePost);
    UserLikePostDTO addUserLikePost(UserLikePostDTO userLikePost);
    void deleteUserLikePost(UserLikePostDTO userLikePost);
}
