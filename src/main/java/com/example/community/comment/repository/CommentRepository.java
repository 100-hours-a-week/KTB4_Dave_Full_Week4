package com.example.community.comment.repository;

import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"userInfo"})
    @Query("""
            select c
            from Comment c
            where c.post.postNum = :postNum
              and c.depth = 0
            """)
    Page<Comment> findByPost_postNum(long postNum, Pageable pageable);

    @Query(
            value = """
                    select new com.example.community.comment.dto.response.CommentResponse(
                        c.commentNum,
                        post.postNum,
                        parent.commentNum,
                        c.depth,
                        userInfo.nickname,
                        userInfo.profileImage,
                        userInfo.deletedAt,
                        c.content,
                        c.childCount,
                        c.editedAt,
                        c.deletedAt,
                        c.writeAt
                    )
                    from Comment c
                    join c.post post
                    join c.userInfo userInfo
                    left join c.comment parent
                    where post.postNum = :postNum
                      and c.depth = 0
                    """,
            countQuery = """
                    select count(c)
                    from Comment c
                    where c.post.postNum = :postNum
                      and c.depth = 0
                    """
    )
    Page<CommentResponse> findFirstPageResponsesByPostNum(
            long postNum,
            Pageable pageable
    );

    @Query("""
            select new com.example.community.comment.dto.response.CommentResponse(
                c.commentNum,
                post.postNum,
                parent.commentNum,
                c.depth,
                userInfo.nickname,
                userInfo.profileImage,
                userInfo.deletedAt,
                c.content,
                c.childCount,
                c.editedAt,
                c.deletedAt,
                c.writeAt
            )
            from Comment c
            join c.post post
            join c.userInfo userInfo
            left join c.comment parent
            where c.commentNum in :commentNums
            """)
    List<CommentResponse> findResponsesByCommentNumIn(
            List<Long> commentNums
    );

    @EntityGraph(attributePaths = {"userInfo"})
    @Query("""
            select c
            from Comment c
            where c.comment.commentNum = :parentNum
            """)
    Page<Comment> findByParentNum(long parentNum, Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo"})
    @Query("""
            select c
            from Comment c
            where c.commentNum = :commentNum
              and c.deletedAt is null
            """)
    Optional<Comment> findByCommentNum(long commentNum);

    @EntityGraph(attributePaths = {"userInfo"})
    Optional<Comment> findByCommentNumAndPost_PostNumAndDeletedAtIsNull(
            long commentNum,
            long postNum
    );
}
