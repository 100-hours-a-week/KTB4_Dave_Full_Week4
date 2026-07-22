package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"userInfo", "postState"})
    Page<Post> findPostByPage(Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.postNum = :postNum and p.deletedAt is null")
    Optional<Post> findByPostNum(Long postNum);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    Page<Post> findPostByUserInfo_ProfileId(long profileId, Pageable pageable);
}
