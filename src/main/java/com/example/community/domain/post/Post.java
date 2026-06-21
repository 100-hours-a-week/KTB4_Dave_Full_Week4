package com.example.community.domain.post;

import com.example.community.domain.user.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "Post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postNum")
    private Long postNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserInfo userInfo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "image")
    private String image;

    @Column(name = "view", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "like", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "report", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "numberOfComment", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "editedAt")
    private Instant editedAt;

    @Column(name = "writeAt", nullable = false)
    private final Instant writeAt = Instant.now();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    public Post(UserInfo userInfo, String title, String content, String image){
        this.userInfo = userInfo;
        this.title = title;
        this.content = content;
        this.image = image;
        deletedAt = null;
        editedAt = null;
    }


    public void update(String title, String content, String image){
        this.title = title;
        this.content = content;
        this.image = image;
        this.editedAt = Instant.now();
        this.version = version+1;
    }

    public void delete(){
        deletedAt = Instant.now();
    }
    public void view(){ viewCount = viewCount +1;}
    public void like(){
        likeCount = likeCount +1;
    }
    public void unlike(){
        likeCount = likeCount -1;
    }

    public void report(){
        reportCount = reportCount + 1;
    }

    public void addComment(){
        commentCount = commentCount + 1;
    }
}
