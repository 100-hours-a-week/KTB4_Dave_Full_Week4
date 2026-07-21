package com.example.community.post.repository;

import com.example.community.post.entity.PostView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    @EntityGraph(attributePaths = {"post"})
    Optional<PostView> findByPost_PostNumAndUserInfo_ProfileId(long postNum, long profileId);
}
