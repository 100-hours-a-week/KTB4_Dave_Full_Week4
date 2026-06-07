package com.example.community.service;

import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.PostLikeResponse;
import com.example.community.domain.post.response.PostListResponse;
import com.example.community.domain.post.response.PostReportResponse;
import com.example.community.domain.post.response.PostResponse;
import com.example.community.domain.token.Token;

public interface PostService {
    PostListResponse getPostsByPage(int index, int offset);
    PostResponse getPost(long postNum);
    PostListResponse getPostsByUserNum(long userNum, int index, int offset);
    PostResponse addPost(Token token, PostRequest postRequest);
    PostResponse updatePost(Token token, PostEditRequest postEditRequest);
    PostLikeResponse likePost(Token token, long postNum);
    PostReportResponse reportPost(long postNum);
    int addComment(long postNum);
    void deletePost(Token token, long postNum);
}
