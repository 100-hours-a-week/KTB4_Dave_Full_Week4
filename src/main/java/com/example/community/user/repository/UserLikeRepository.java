package com.example.community.user.repository;

import com.example.community.user.entity.UserLikePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLikeRepository extends JpaRepository<UserLikePost, Long> {
    @EntityGraph(attributePaths = {"userInfo", "post", "post.postState"})
    Page<UserLikePost> findByUserInfo_ProfileId(long profileId, Pageable pageable);

    boolean existsByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
    Optional<UserLikePost> findByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
}
