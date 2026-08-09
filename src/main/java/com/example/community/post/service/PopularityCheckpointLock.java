package com.example.community.post.service;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularityCheckpointLock {
    private final PopularityAggregationCheckpointRepository checkpointRepository;

    public PopularityAggregationCheckpoint acquire() {
        return checkpointRepository
                .findByJobNameForUpdate(
                        PopularityAggregationCheckpoint.JOB_NAME
                )
                .orElseGet(() -> checkpointRepository.saveAndFlush(
                        new PopularityAggregationCheckpoint(
                                PopularityAggregationCheckpoint.JOB_NAME
                        )
                ));
    }
}
