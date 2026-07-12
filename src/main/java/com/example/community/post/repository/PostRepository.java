package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.deletedAt is null order by p.postNum desc")
    Page<Post> findPostByPage(Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.deletedAt is null order by p.postState.likeCount desc")
    Page<Post> findPostByPageOrderByLikeCount(Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.deletedAt is null order by p.postState.viewCount desc")
    Page<Post> findPostByPageOrderByViewCount(Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.postNum = :postNum and p.deletedAt is null")
    Optional<Post> findByPostNum(Long postNum);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.postNum in :postNums and p.deletedAt is null")
    List<Post> findByPostNumIn(List<Long> postNums);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.userInfo.profileId = :profileId and p.deletedAt is null order by p.postNum desc")
    Page<Post> findByUserInfo_ProfileIdOrderByPostNumDesc(long profileId, Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.userInfo.profileId = :profileId and p.deletedAt is null order by p.postState.likeCount desc")
    Page<Post> findByUserInfo_ProfileIdOrderByLikeCountDesc(long profileId, Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.userInfo.profileId = :profileId and p.deletedAt is null order by p.postState.viewCount desc")
    Page<Post> findByUserInfo_ProfileIdOrderByViewCountDesc(long profileId, Pageable pageable);
}
