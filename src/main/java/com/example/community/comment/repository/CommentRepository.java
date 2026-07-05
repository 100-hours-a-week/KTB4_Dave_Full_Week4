package com.example.community.comment.repository;

import com.example.community.comment.dto.CommentDTO;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    CommentDTO addComment(CommentDTO comment);
    List<CommentDTO> getCommentsByPostNum(long postNum);
    Optional<CommentDTO> getComment(long commentNum);
    long getCommentCount();
    CommentDTO updateComment(long commentNum, String content);
    void deleteComment(long commentNum);
}
