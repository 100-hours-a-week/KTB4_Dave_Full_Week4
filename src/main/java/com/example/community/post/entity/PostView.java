package com.example.community.post.entity;

import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "PostView")
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

    public boolean view(){
        Instant twentyFourHoursAgo = Instant.now().minus(Duration.ofHours(24));

        if (viewAt.isAfter(twentyFourHoursAgo)) {
            return false;
        }
        viewUpdate();
        return true;
    }

    private void viewUpdate(){
        viewAt = Instant.now();
    }
}
