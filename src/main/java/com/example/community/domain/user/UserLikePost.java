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
@Table(name="UserLikePost")
public class UserLikePost {
    @EmbeddedId
    private UserLikePostId id;

    @MapsId("userNum")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", referencedColumnName = "userNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SignInfo signInfo;

    @MapsId("postNum")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", referencedColumnName = "postNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    public UserLikePost(SignInfo signInfo, Post post) {
        this.signInfo = signInfo;
        this.post = post;
        this.id = new UserLikePostId(
                signInfo.getUserNum(),
                post.getPostNum()
        );
    }
}
