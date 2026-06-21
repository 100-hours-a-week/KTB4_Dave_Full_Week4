package com.example.community.domain.comment;

import com.example.community.domain.exception.BadRequestException;
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
    private int version;

    public Comment(Post post, Comment comment, UserInfo userInfo, String content){
        if(post == null || userInfo == null || comment == null || content.isBlank()){
            throw new IllegalArgumentException("필수 인자가 비어있습니다.");
        }
        if (comment.getDepth() >= 3) {
            throw new BadRequestException("답글을 달 수 없는 댓글입니다.");
        }
        commentNum = null;
        this.post = post;
        this.comment = comment;
        this.depth = comment.getDepth()+1;
        this.userInfo = userInfo;
        this.content = content;
    }

    public Comment(Post post,UserInfo userInfo, String content){
        if(post == null || userInfo == null || content.isBlank()){
            throw new IllegalArgumentException("필수 인자가 비어있습니다.");
        }
        commentNum = null;
        this.post = post;
        this.comment = null;
        this.depth = 0;
        this.userInfo = userInfo;
        this.content = content;
    }

    public void update(String content){
        this.content = content;
        editedAt = Instant.now();
    }

    public void delete(){
        this.deletedAt = Instant.now();
    }
}
