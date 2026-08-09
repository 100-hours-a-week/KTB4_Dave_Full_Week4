package com.example.community.post.repository;

import com.example.community.post.dto.query.PostStateData;
import com.example.community.post.entity.PostState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostStateRepository extends JpaRepository<PostState, Long> {
    @Query("""
            select new com.example.community.post.dto.query.PostStateData(
                state.viewCount,
                state.likeCount,
                coalesce(state.reportCount, 0),
                state.commentCount
            )
            from PostState state
            join state.post post
            where state.postNum = :postNum
              and post.deletedAt is null
            """)
    Optional<PostStateData> findDataByPostNum(
            @Param("postNum") long postNum
    );

    @Modifying
    @Query("""
            update PostState state
            set state.viewCount = state.viewCount + 1
            where state.postNum = :postNum
            """)
    int incrementViewCount(@Param("postNum") long postNum);
}
