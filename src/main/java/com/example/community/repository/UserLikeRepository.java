package com.example.community.repository;

import com.example.community.domain.user.UserLikePost;

import java.util.List;

public interface UserLikeRepository {
    List<UserLikePost> getUserLikePosts(long userNum);
    boolean isUserLikePost(UserLikePost userLikePost);
    UserLikePost addUserLikePost(UserLikePost userLikePost);
    void deleteUserLikePost(UserLikePost userLikePost);
}
