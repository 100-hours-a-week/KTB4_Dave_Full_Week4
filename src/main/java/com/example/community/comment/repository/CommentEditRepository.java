package com.example.community.comment.repository;

import com.example.community.comment.entity.CommentEditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentEditRepository extends JpaRepository<CommentEditRecord, Long> {
    Page<CommentEditRecord> findByComment_CommentNumOrderByEditIdDesc(Long commentNum, Pageable pageable);
}
