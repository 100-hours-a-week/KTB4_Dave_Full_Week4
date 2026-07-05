package com.example.community.post.repository;

import com.example.community.post.entity.PostEditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostEditJpaRepository extends JpaRepository<PostEditRecord, Long> {
    List<PostEditRecord> findByPost_PostNumOrderByEditIdDesc(Long postNum);
}
