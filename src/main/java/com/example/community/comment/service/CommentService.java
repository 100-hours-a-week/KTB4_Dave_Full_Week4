package com.example.community.comment.service;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentListResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.resolver.SignUserInfo;

import java.util.List;

public interface CommentService {
    void checkUserAuthority(SignUserInfo signUserInfo, long commentNum);
    CommentAddResponse addCommentToPost(SignUserInfo signUserInfo, long postNum, CommentToPostRequest commentRequest);
    CommentAddResponse addCommentToComment(SignUserInfo signUserInfo, long postNum, CommentToCommentRequest commentRequest);
    List<CommentListResponse> getPostCommentList(long postNum);
    CommentResponse updateComment(SignUserInfo signUserInfo, long commentNum, CommentEditRequest commentEditRequest);
    void deleteComment(SignUserInfo signUserInfo, long commentNum);
}
