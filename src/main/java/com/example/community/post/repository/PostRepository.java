package com.example.community.post.repository;

import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.response.PopularPostTitleResponse;
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
    @Query("select p from Post p where p.deletedAt is null")
    Page<Post> findPostByPage(Pageable pageable);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.postNum = :postNum and p.deletedAt is null")
    Optional<Post> findByPostNum(Long postNum);

    @Query("""
            select new com.example.community.post.dto.response.PopularPostTitleResponse(
                p.postNum,
                userInfo.nickname,
                userInfo.profileImage,
                userInfo.deletedAt,
                p.title,
                p.writeAt
            )
            from Post p
            join p.userInfo userInfo
            where p.deletedAt is null
              and p.postNum in :postNums
            """)
    List<PopularPostTitleResponse> findPopularPostTitlesByPostNumIn(
            List<Long> postNums
    );

    @Query("""
            select new com.example.community.post.dto.query.PostBodyData(
                p.postNum,
                userInfo.nickname,
                userInfo.profileImage,
                userInfo.deletedAt,
                p.title,
                p.content,
                p.image,
                p.editedAt,
                p.writeAt
            )
            from Post p
            join p.userInfo userInfo
            where p.postNum = :postNum
              and p.deletedAt is null
            """)
    Optional<PostBodyData> findPostBodyDataByPostNum(long postNum);

    @EntityGraph(attributePaths = {"userInfo", "postState"})
    @Query("select p from Post p where p.userInfo.profileId = :profileId and p.deletedAt is null")
    Page<Post> findPostByUserInfo_ProfileId(long profileId, Pageable pageable);
}
