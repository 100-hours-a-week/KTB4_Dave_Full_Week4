package com.example.community.service;

import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;

import java.util.List;

public interface PostService {
    void checkUserAuthority(String token, long postNum);
    List<PostTitleResponse> getAllPosts();
    PostListResponse getPostsByPage(int index, int offset);
    PostResponse getPost(long postNum);
    PostListResponse getPostsByUserNum(long userNum, int index, int offset);
    PostResponse addPost(String token, PostRequest postRequest);
    PostResponse updatePost(String token, long postNum, PostEditRequest postEditRequest);
    PostLikeResponse likePost(String token, long postNum);
    PostReportResponse reportPost(long postNum);
    void deletePost(String token, long postNum);
}
