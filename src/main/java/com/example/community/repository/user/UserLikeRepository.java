package com.example.community.repository.user;

import com.example.community.domain.user.UserLikePostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserLikeRepository {
    Page<UserLikePostDTO> getUserLikePosts(long profileId, Pageable pageable);
    boolean isUserLikePost(UserLikePostDTO userLikePost);
    void addUserLikePost(UserLikePostDTO userLikePost);
    void deleteUserLikePost(UserLikePostDTO userLikePost);
}
