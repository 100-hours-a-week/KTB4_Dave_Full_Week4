package com.example.community.post.entity;

import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "PostView",
        uniqueConstraints = {
            @UniqueConstraint(
                name = "UK_PostView_postNum_profileId",
                columnNames = {"postNum", "profileId"}
            )
    })
public class PostView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postViewId")
    private Long postViewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    private UserInfo userInfo;

    @Column(name = "viewAt")
    private Instant viewAt = Instant.now();

    public PostView(Post post, UserInfo userInfo){
        this.post = post;
        this.userInfo = userInfo;
        this.post.view();
    }

    public void view(){
        Instant twentyFourHoursAgo = Instant.now().minus(Duration.ofHours(24));

        if (viewAt.isAfter(twentyFourHoursAgo)) {
            return;
        }
        viewUpdate();
        post.view();
    }

    private void viewUpdate(){
        viewAt = Instant.now();
    }
}
