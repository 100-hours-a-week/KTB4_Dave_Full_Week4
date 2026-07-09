package com.example.community.temporaryPost.entity;

import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "TemporaryPost")
public class TemporaryPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="temporaryId")
    private Long temporaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    private UserInfo userInfo;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "image")
    private String image;

    @Column(name = "writeAt", nullable = false)
    private Instant writeAt = Instant.now();

    public TemporaryPost(UserInfo userInfo){
        this.userInfo = userInfo;
        writeAt = Instant.now();
    }

    public void update(String title, String content, String image){
        this.title = title;
        this.content = content;
        this.image = image;
        writeAt = Instant.now();
    }
}
