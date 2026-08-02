package com.example.community.post.repository;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PopularityAggregationCheckpointRepository
        extends JpaRepository<PopularityAggregationCheckpoint, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select checkpoint
            from PopularityAggregationCheckpoint checkpoint
            where checkpoint.jobName = :jobName
            """)
    Optional<PopularityAggregationCheckpoint> findByJobNameForUpdate(
            @Param("jobName") String jobName
    );
}
