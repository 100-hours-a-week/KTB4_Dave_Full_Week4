package com.example.community.user.service;

import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.service.PostSortPolicy;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.repository.UserLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikedPostQueryService {
    private final UserLikeRepository userLikeRepository;

    @Transactional(readOnly = true)
    public PostPageResponse getMyLikePosts(
            SignUserInfo signUserInfo,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                PostSortPolicy.forLikedPosts(sort)
        );
        Page<UserLikePost> likedPosts = userLikeRepository
                .findByUserInfo_ProfileId(
                        signUserInfo.profileId(),
                        pageable
                );
        return PostPageResponse.fromUserLike(likedPosts);
    }
}
