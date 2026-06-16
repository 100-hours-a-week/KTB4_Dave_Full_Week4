package com.example.community.service;

import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentResponse;

import java.util.List;

public interface CommentService {
    void checkUserAuthority(String token, long commentNum);
    CommentAddResponse addCommentToPost(String token, long postNum, CommentToPostRequest commentRequest);
    CommentAddResponse addCommentToComment(String  token, long postNum, CommentToCommentRequest commentRequest);
    List<CommentResponse> getPostCommentList(long postNum);
    CommentResponse updateComment(String token, long commentNum, CommentEditRequest commentEditRequest);
    void deleteComment(String token, long commentNum);
}
