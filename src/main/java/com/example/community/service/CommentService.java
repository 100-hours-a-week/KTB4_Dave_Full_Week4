package com.example.community.service;

import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.resolver.SignUserInfo;

import java.util.List;

public interface CommentService {
    void checkUserAuthority(SignUserInfo signUserInfo, long commentNum);
    CommentAddResponse addCommentToPost(SignUserInfo signUserInfo, long postNum, CommentToPostRequest commentRequest);
    CommentAddResponse addCommentToComment(SignUserInfo signUserInfo, long postNum, CommentToCommentRequest commentRequest);
    List<CommentResponse> getPostCommentList(long postNum);
    CommentResponse updateComment(SignUserInfo signUserInfo, long commentNum, CommentEditRequest commentEditRequest);
    void deleteComment(SignUserInfo signUserInfo, long commentNum);
}
