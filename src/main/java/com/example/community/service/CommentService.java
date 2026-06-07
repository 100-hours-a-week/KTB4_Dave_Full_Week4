package com.example.community.service;

import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentListResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.domain.token.Token;

import java.util.List;

public interface CommentService {
    void checkUserAuthority(Token token, long commentNum);
    CommentAddResponse addCommentToPost(Token token, long postNum, CommentToPostRequest commentRequest);
    CommentAddResponse addCommentToComment(Token token, long postNum, CommentToCommentRequest commentRequest);
    List<CommentResponse> getPostCommentList(long postNum);
    CommentResponse updateComment(Token token, long commentNum, CommentEditRequest commentEditRequest);
    void deleteComment(Token token, long commentNum);
}
