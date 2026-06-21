package com.example.community.repository.comment;

import com.example.community.domain.comment.Comment;
import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.comment.CommentEditRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentEditJpaAdapterRepository {
    private final CommentEditJpaRepository commentEditJpaRepository;
    private final CommentJpaRepository commentJpaRepository;

    public List<CommentEditRecord> findByCommentNum(Long commentNum){
        return commentEditJpaRepository.findByComment_CommentNumOrderByEditIdDesc(commentNum);
    }

    public void addCommentEdit(CommentDTO commentDTO){
        Comment comment = commentJpaRepository.findByCommentNum(commentDTO.getCommentNum())
                .orElse(null);
        if(comment == null) return;

        CommentEditRecord commentEditRecord = new CommentEditRecord(comment);
        commentEditJpaRepository.save(commentEditRecord);
    }

    public void deleteCommentEdit(long commentNum){
        List<CommentEditRecord> commentEditRecords = commentEditJpaRepository.findByComment_CommentNumOrderByEditIdDesc(commentNum);
        commentEditJpaRepository.deleteAll(commentEditRecords);
    }
}
