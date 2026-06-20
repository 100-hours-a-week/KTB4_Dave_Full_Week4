package com.example.community.domain.user;

import com.example.community.domain.post.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserInfo userInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    public UserLikePost(UserInfo userInfo, Post post) {
        if(userInfo == null || post == null){
            throw new IllegalArgumentException("null이 아닌 인자가 전달돼야 함");
        }
        this.userInfo = userInfo;
        this.post = post;
    }
}
