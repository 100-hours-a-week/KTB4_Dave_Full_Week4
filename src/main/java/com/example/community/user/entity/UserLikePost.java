package com.example.community.user.entity;

import com.example.community.post.entity.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name="UserLikePost",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_UserLikePost_userNum_postNum",
                        columnNames = {"profileId", "postNum"}
                )
        })
public class UserLikePost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likeId")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    private UserInfo userInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    private Post post;

    public UserLikePost(UserInfo userInfo, Post post) {
        if(userInfo == null || post == null){
            throw new IllegalArgumentException("null이 아닌 인자가 전달돼야 함");
        }
        this.userInfo = userInfo;
        this.post = post;
        this.post.like();
    }
}
