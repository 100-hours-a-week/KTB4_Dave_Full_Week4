package com.example.community.repository;

import com.example.community.domain.comment.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    Comment addComment(Comment comment);
    List<Comment> getCommentsByPostNum(long postNum);
    Optional<Comment> getComment(long commentNum);
    int getCommentCount();
    Optional<Comment> updateComment(long commentNum, String content);
    void deleteComment(long commentNum);
}
