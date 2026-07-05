package com.example.community.comment.repository;

import com.example.community.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentJpaRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_postNum(Long postNum);
    Optional<Comment> findByCommentNum(Long commentNum);
}
