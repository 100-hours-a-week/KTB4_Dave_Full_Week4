package com.example.community.service.post;

import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;
import com.example.community.resolver.SignUserInfo;


public interface PostService {
    void checkUserAuthority(SignUserInfo signUserInfo, long postNum);
    PostSliceResponse getPostsByPage(int page, int size);
    PostResponse getPost(long postNum);
    PostPageResponse getPostsByProfileId(long profileId, int page, int size);
    PostPageResponse getLikePosts(long profileId, int page, int size);
    PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest);
    PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest);
    PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum);
    PostReportResponse reportPost(long postNum);
    void deletePost(SignUserInfo signUserInfo, long postNum);
}
