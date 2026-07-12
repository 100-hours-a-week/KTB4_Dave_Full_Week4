package com.example.community.comment.repository;

import com.example.community.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"userInfo"})
    @Query("select c from Comment c " +
            "where c.post.postNum = :postNum and c.depth = 0 order by c.commentNum desc")
    Page<Comment> findByPost_postNum(long postNum, Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo"})
    @Query("select c from Comment c where c.comment.commentNum = :parentNum")
    Page<Comment> findByParentNum(long parentNum, Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo"})
    @Query("select c from Comment c where c.commentNum = :commentNum")
    Optional<Comment> findByCommentNum(long commentNum);
}
