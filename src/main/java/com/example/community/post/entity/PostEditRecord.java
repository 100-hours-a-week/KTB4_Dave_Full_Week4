package com.example.community.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "PostEditRecord",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_PostEdit_postNum_version",
                        columnNames = {"postNum", "version"}
                )
        }
)
public class PostEditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postEditId")
    private Long editId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    private Post post;

    @Column(name = "version")
    private Integer version;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "image")
    private String image;

    @Column(name = "writeAt", nullable = false)
    private Instant writeAt;

    public PostEditRecord(Post post){
        if(post == null){
            throw new IllegalArgumentException("post가 null");
        }
        this.post = post;
        this.version = post.getVersion();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.image = post.getImage();
        this.writeAt = post.getEditedAt() == null ? post.getWriteAt() : post.getEditedAt();
    }
}
