package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PostJpaRepository extends JpaRepository<Post, Long> {
    @Query("select p from Post p join fetch p.userInfo where p.deletedAt is null order by p.postNum desc")
    Page<Post> findPostByPage(Pageable pageable);

    @Query("select p from Post p join fetch p.userInfo where p.postNum = :postNum")
    Optional<Post> findByPostNum(Long postNum);

    @Query("select p from Post p join fetch p.userInfo where p.postNum in :postNums")
    List<Post> findByPostNumIn(List<Long> postNums);

    @Query("select p from Post p join fetch p.userInfo where p.userInfo.profileId = profielId")
    Page<Post> findByUserInfo_ProfileIdOrderByPostNumDesc(Long profileId, Pageable pageable);

}
