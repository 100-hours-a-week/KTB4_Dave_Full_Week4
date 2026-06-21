package com.example.community.repository.comment;

import com.example.community.domain.comment.CommentEditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentEditJpaRepository extends JpaRepository<CommentEditRecord, Long> {
    List<CommentEditRecord> findByComment_CommentNumOrderByEditIdDesc(Long commentNum);
}
