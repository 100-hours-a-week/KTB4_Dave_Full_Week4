package com.example.community.repository.user;

import com.example.community.domain.user.UserLikePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLikeJpaRepository extends JpaRepository<UserLikePost, Long> {
    Page<UserLikePost> findByUserInfo_ProfileId(Long profileId, Pageable pageable);
    boolean existsByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
    Optional<UserLikePost> findByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
}
