package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.post.dto.response.PostDetailResponse;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.dto.response.PostSliceResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostQueryService {
    private final PostRepository postRepository;
    private final PostDetailReadService postDetailReadService;
    private final PostViewRecordingService postViewRecordingService;
    private final PopularPostSnapshotService popularPostSnapshotService;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByPage(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                PostSortPolicy.forPosts(sort)
        );
        Page<Post> posts = postRepository.findPostByPage(pageable);
        return PostPageResponse.from(posts, imageUrlBuilder);
    }

    public PostDetailResponse getPost(
            SignUserInfo signUserInfo,
            long postNum
    ) {
        PostDetailData detail = postDetailReadService.read(postNum);
        if (detail.state().isBlind()) {
            throw new ForbiddenException("신고 처리된 게시글");
        }
        recordViewIfAuthenticated(signUserInfo, postNum, detail);
        return PostDetailResponse.from(
                detail.body(),
                detail.state(),
                imageUrlBuilder
        );
    }

    @Transactional(readOnly = true)
    public PostPageResponse getMyPosts(
            SignUserInfo signUserInfo,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                PostSortPolicy.forPosts(sort)
        );
        Page<Post> posts = postRepository.findPostByUserInfo_ProfileId(
                signUserInfo.profileId(),
                pageable
        );
        return PostPageResponse.from(posts, imageUrlBuilder);
    }

    @Transactional(readOnly = true)
    public PostSliceResponse getTop10PopularPosts() {
        return PostSliceResponse.from(popularPostSnapshotService.getSnapshot());
    }

    private void recordViewIfAuthenticated(
            SignUserInfo signUserInfo,
            long postNum,
            PostDetailData detail
    ) {
        if (signUserInfo == null || signUserInfo.profileId() == null) {
            return;
        }
        postViewRecordingService.record(
                signUserInfo.profileId(),
                postNum,
                detail.body().writeAt()
        );
    }
}
