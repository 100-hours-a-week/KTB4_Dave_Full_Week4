package com.example.community.post.repository;

import com.example.community.post.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    boolean existsByPost_PostNumAndUserInfo_ProfileId(long postNum, long profileId);
}
