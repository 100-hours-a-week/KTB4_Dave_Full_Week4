package com.example.community.post.repository;

import com.example.community.post.entity.PostPopularityStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostPopularityStatRepository extends JpaRepository<PostPopularityStat, Long> {
    @Query("""
        select stat.postNum
        from PostPopularityStat stat
        join stat.post post
        join post.postState postState
        where post.deletedAt is null
          and postState.reportCount <= 5
          and post.writeAt >= :candidateSince
          and stat.viewCount5m > 0
        """)
    List<Long> findPostNumsWithNonZeroFiveMinuteCount(
            @Param("candidateSince") Instant candidateSince
    );

    @Query("""
            select stat.postNum
            from PostPopularityStat stat
            join stat.post post
            join post.postState postState
            where post.deletedAt is null
              and postState.reportCount <= 5
              and post.writeAt >= :candidateSince
            order by stat.popularityScore desc,
                     stat.viewCount5m desc,
                     stat.viewCount30m desc,
                     stat.postNum desc
            """)
    List<Long> findPopularPostNums(
            @Param("candidateSince") Instant candidateSince,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            delete from PostPopularityStat stat
            where stat.postNum in (
                select post.postNum
                from Post post
                where post.writeAt < :candidateSince
            )
            """)
    int deleteAllByPostWriteAtBefore(
            @Param("candidateSince") Instant candidateSince
    );
}
