package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    @EntityGraph(attributePaths = {"post"})
    Optional<PostView> findByPost_PostNumAndUserInfo_ProfileId(long postNum, long profileId);

    @EntityGraph(attributePaths = {"post", "post.userInfo"})
    @Query("""
        select pv.post
        from PostView pv
        where pv.viewAt >= :startTime
        group by pv.post
        order by count(pv) desc
    """)
    Page<Post> findPopularPosts(Instant startTime, Pageable pageable);
}
