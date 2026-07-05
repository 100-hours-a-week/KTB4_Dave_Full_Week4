package com.example.community.post.service;

import com.example.community.temporaryPost.dto.response.request.PostRequest;
import com.example.community.post.dto.response.*;
import com.example.community.resolver.SignUserInfo;

import java.io.IOException;


public interface PostService {
    void checkUserAuthority(SignUserInfo signUserInfo, long postNum);
    PostSliceResponse getPostsByPage(int page, int size);
    PostResponse getPost(long postNum);
    PostPageResponse getPostsByProfileId(long profileId, int page, int size);
    PostPageResponse getLikePosts(long profileId, int page, int size);
    PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) throws IOException;
    PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException;
    PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum);
    boolean isLikePost(SignUserInfo signUserInfo, long postNum);
    PostReportResponse reportPost(long postNum);
    void deletePost(SignUserInfo signUserInfo, long postNum);
}
