package com.example.community.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PopularityAggregationCheckpoint")
public class PopularityAggregationCheckpoint {
    public static final String JOB_NAME = "POPULAR_POSTS";

    @Id
    @Column(name = "jobName", length = 50)
    private String jobName;

    @Column(name = "lastProcessedEndAt")
    private Instant lastProcessedEndAt;

    public PopularityAggregationCheckpoint(String jobName) {
        this.jobName = jobName;
    }

    public void advanceTo(Instant windowEndAt) {
        this.lastProcessedEndAt = windowEndAt;
    }
}
