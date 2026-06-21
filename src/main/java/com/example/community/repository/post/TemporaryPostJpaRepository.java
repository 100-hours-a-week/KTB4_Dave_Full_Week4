package com.example.community.repository.post;

import com.example.community.domain.post.TemporaryPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemporaryPostJpaRepository extends JpaRepository<TemporaryPost, Long> {
    Optional<TemporaryPost> findByTemporaryId(Long temporaryId);
    List<TemporaryPost> findByUserInfo_ProfileId(Long profileId);

}
