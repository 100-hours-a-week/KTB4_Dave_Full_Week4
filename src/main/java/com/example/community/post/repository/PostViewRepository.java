package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    Optional<PostView> findByPost_PostNumAndUserInfo_ProfileId(long postNum, long profileId);

    @Query("""
        select pv.post.postNum
        from PostView pv
        where pv.viewAt >= :startTime
        group by pv.post.postNum
        order by count(pv) desc
    """)
    Slice<Long> findPopularPosts(Instant startTime, Pageable pageable);
}
