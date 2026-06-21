package com.example.community.service.post;

import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;
import com.example.community.resolver.SignUserInfo;


public interface PostService {
    void checkUserAuthority(SignUserInfo signUserInfo, long postNum);
    PostSliceResponse getPostsByPage(int index, int offset);
    PostResponse getPost(long postNum);
    PostPageResponse getPostsByProfileId(long profileId, int index, int offset);
    PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest);
    PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostEditRequest postEditRequest);
    PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum);
    PostReportResponse reportPost(long postNum);
    void deletePost(SignUserInfo signUserInfo, long postNum);
}
