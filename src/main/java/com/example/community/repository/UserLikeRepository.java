package com.example.community.repository;

import com.example.community.domain.user.UserLikePost;

import java.util.List;
import java.util.Optional;

public interface UserLikeRepository {
    List<UserLikePost> getUserLikePosts(long userNum);
    boolean isUserLikePost(UserLikePost userLikePost);
    Optional<UserLikePost> addUserLikePost(UserLikePost userLikePost);
    void deleteUserLikePost(UserLikePost userLikePost);
}
