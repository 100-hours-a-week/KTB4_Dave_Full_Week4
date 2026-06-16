package com.example.community.repository;

import com.example.community.domain.comment.CommentDTO;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    CommentDTO addComment(CommentDTO comment);
    List<CommentDTO> getCommentsByPostNum(long postNum);
    Optional<CommentDTO> getComment(long commentNum);
    int getCommentCount();
    Optional<CommentDTO> updateComment(long commentNum, String content);
    void deleteComment(long commentNum);
}
