package com.example.community.repository.user;

import com.example.community.domain.user.UserLikePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLikeJpaRepository extends JpaRepository<UserLikePost, Long> {
    List<UserLikePost> findByUserInfo_ProfileId(Long profileId);
    boolean existsByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
    Optional<UserLikePost> findByUserInfo_ProfileIdAndPost_PostNum(Long profileId, Long postNum);
}
