package com.example.community.comment.entity;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentNum")
    private Comment comment;

    @Column(name = "depth")
    private Integer depth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    private UserInfo userInfo;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "editedAt")
    private Instant editedAt;

    @Column(name = "writeAt", nullable = false)
    private final Instant writeAt = Instant.now();

    @Column(name = "version")
    @Version
    private int version;

    @Column(name = "childCount")
    private long childCount = 0;

    public Comment(Post post, Comment comment, UserInfo userInfo, String content){
        if(post == null || userInfo == null || comment == null || content.isBlank()){
            throw new IllegalArgumentException("필수 인자가 비어있습니다.");
        }
        if (comment.getDepth() >= 3) {
            throw new BadRequestException("답글을 달 수 없는 댓글입니다.");
        }
        comment.addChild();
        post.getPostState().addComment();
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
        post.getPostState().addComment();
        commentNum = null;
        this.post = post;
        this.comment = null;
        this.depth = 0;
        this.userInfo = userInfo;
        this.content = content;
    }

    public String getContent(){
        return isDeleted() ? "삭제된 댓글입니다." : content;
    }

    public void update(String content){
        this.content = content;
        editedAt = Instant.now();
    }

    private void addChild(){
        this.childCount++;
    }

    private void deleteChild(){
        this.childCount--;
    }

    public void delete(){
        if(this.comment != null){
            this.comment.deleteChild();
        }
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted(){
        return this.deletedAt == null;
    }
}
