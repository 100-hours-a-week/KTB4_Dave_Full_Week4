package com.example.community.post.repository;

import com.example.community.post.entity.PostPopularityStat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostPopularityStatRepository extends JpaRepository<PostPopularityStat, Long> {
    @Query("""
            select stat.postNum
            from PostPopularityStat stat
            where stat.viewCount5m > 0
            """)
    List<Long> findPostNumsWithNonZeroFiveMinuteCount();

    @Query("""
            select stat.postNum
            from PostPopularityStat stat
            join stat.post post
            join post.postState postState
            where post.deletedAt is null
              and postState.reportCount <= 5
            order by stat.popularityScore desc,
                     stat.viewCount5m desc,
                     stat.viewCount30m desc,
                     stat.postNum desc
            """)
    List<Long> findPopularPostNums(Pageable pageable);
}
