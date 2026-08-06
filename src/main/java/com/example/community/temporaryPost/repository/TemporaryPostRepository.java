package com.example.community.temporaryPost.repository;

import com.example.community.temporaryPost.entity.TemporaryPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemporaryPostRepository extends JpaRepository<TemporaryPost, Long> {
    Optional<TemporaryPost> findByTemporaryId(Long temporaryId);
    Optional<TemporaryPost> findByTemporaryIdAndUserInfo_ProfileId(
            Long temporaryId,
            Long profileId
    );
    List<TemporaryPost> findByUserInfo_ProfileId(Long profileId);

}
