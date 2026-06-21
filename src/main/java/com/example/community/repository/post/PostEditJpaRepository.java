package com.example.community.repository.post;

import com.example.community.domain.post.PostEditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostEditJpaRepository extends JpaRepository<PostEditRecord, Long> {
    List<PostEditRecord> findByPost_PostNumOrderByEditIdDesc(Long postNum);
}
