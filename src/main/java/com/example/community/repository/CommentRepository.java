package com.example.community.repository;

import com.example.community.domain.comment.Comment;
import com.example.community.domain.comment.request.CommentEditRequest;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment addComment(Comment comment);
    List<Comment> getCommentsByPostNum(long postNum);
    Optional<Comment> updateComment(long commentNum, String content);
    void deleteComment(long commentNum);
}
