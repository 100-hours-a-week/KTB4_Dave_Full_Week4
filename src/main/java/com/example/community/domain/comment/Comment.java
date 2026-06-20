package com.example.community.domain.comment;

import com.example.community.domain.post.Post;
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
@Table(name = "Comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentNum")
    private Long commentNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Column(name = "depth")
    private Integer depth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserInfo userInfo;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "editedAt")
    private Instant editedAt;

    @Column(name = "writeAt", nullable = false)
    private final Instant writeAt = Instant.now();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    public void update(String content){
        this.content = content;
        editedAt = Instant.now();
        version = version+1;
    }

    public void delete(){
        this.deletedAt = Instant.now();
    }
}
