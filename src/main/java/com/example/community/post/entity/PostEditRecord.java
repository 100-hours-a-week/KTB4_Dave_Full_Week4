package com.example.community.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public PostEditRecord(Post post, Integer version, String title, String content, String image, Instant writeAt){
        if(post == null){
            throw new IllegalArgumentException("post가 null");
        }
        if(version == null || version < 1){
            throw new IllegalArgumentException("version은 양수여야 함");
        }
        if(title == null || title.isBlank() || content == null || content.isBlank()){
            throw new IllegalArgumentException("제목과 내용은 비어있으면 안됨");
        }
        if(writeAt == null){
            throw new IllegalArgumentException("writeTime이 null");
        }
        this.post = post;
        this.version = version;
        this.title = title;
        this.content = content;
        this.image = image;
        this.writeAt = writeAt;
    }
}
