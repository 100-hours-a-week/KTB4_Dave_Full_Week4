package com.example.community.service;

import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;
import com.example.community.domain.token.Token;

import java.util.List;

public interface PostService {
    void checkUserAuthority(Token token, long postNum);
    List<PostTitleResponse> getAllPosts();
    PostListResponse getPostsByPage(int index, int offset);
    PostResponse getPost(long postNum);
    PostListResponse getPostsByUserNum(long userNum, int index, int offset);
    PostResponse addPost(Token token, PostRequest postRequest);
    PostResponse updatePost(Token token, long postNum, PostEditRequest postEditRequest);
    PostLikeResponse likePost(Token token, long postNum);
    PostReportResponse reportPost(long postNum);
    void deletePost(Token token, long postNum);
}
