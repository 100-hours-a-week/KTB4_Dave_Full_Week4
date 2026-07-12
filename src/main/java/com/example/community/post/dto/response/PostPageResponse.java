package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserLikePost;
import org.springframework.data.domain.Page;

import java.util.List;

public record PostPageResponse(
        List<PostTitleResponse> postTitleResponses,
        int page,
        int pageSize,
        int postCount,
        long totalCount,
        int totalPage
) {
    public static PostPageResponse from(Page<Post> postPage){
        return new PostPageResponse(
                postPage.getContent().stream().map(PostTitleResponse::from).toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getNumberOfElements(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
        );
    }

    public static PostPageResponse fromUserLike(Page<UserLikePost> userLikePosts){
        return new PostPageResponse(
                userLikePosts.getContent().stream().map(PostTitleResponse::from).toList(),
                userLikePosts.getNumber(),
                userLikePosts.getSize(),
                userLikePosts.getNumberOfElements(),
                userLikePosts.getTotalElements(),
                userLikePosts.getTotalPages()
        );
    }

    public static PostPageResponse adminFrom(Page<Post> postPage){
        return new PostPageResponse(
                postPage.getContent().stream().map(PostTitleResponse::adminFrom).toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getNumberOfElements(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
        );
    }
}
