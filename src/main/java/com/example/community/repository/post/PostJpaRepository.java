package com.example.community.repository.post;

import com.example.community.domain.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PostJpaRepository extends JpaRepository<Post, Long> {
    Slice<Post> findAllByOrderByPostNumDesc(Pageable pageable);
    Optional<Post> findByPostNum(Long postNum);
    Page<Post> findByUserInfo_ProfileIdOrderByPostNumDesc(Long profileId, Pageable pageable);

}
