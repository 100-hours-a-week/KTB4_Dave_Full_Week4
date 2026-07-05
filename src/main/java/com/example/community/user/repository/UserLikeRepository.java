package com.example.community.user.repository;

import com.example.community.user.dto.UserLikePostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserLikeRepository {
    Page<UserLikePostDTO> getUserLikePosts(long profileId, Pageable pageable);
    boolean isUserLikePost(UserLikePostDTO userLikePost);
    void addUserLikePost(UserLikePostDTO userLikePost);
    void deleteUserLikePost(UserLikePostDTO userLikePost);
}
