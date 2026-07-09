package com.example.community.post.repository;

import com.example.community.post.entity.PostEditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostEditRepository extends JpaRepository<PostEditRecord, Long> {
    Page<PostEditRecord> findByPost_PostNumOrderByEditIdDesc(Long postNum, Pageable pageable);
}
