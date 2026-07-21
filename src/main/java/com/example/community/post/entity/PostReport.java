package com.example.community.post.entity;

import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;

@Entity
@Table(
        name = "PostReport",
        uniqueConstraints = {
                @UniqueConstraint(
                    name = "UK_PostReport_postNum_profileId",
                    columnNames = {"postNum", "profileId"}
                )
        }
)
public class PostReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    private UserInfo userInfo;

    public PostReport(Post post, UserInfo userInfo){
        reportId = null;
        this.post = post;
        this.userInfo = userInfo;
        this.post.report();
    }
}
