package com.example.community.post.entity;

import com.example.community.user.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private UserInfo userInfo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "image")
    private String image;


    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL)
    private PostState postState;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "editedAt")
    private Instant editedAt;

    @Column(name = "writeAt", nullable = false)
    private final Instant writeAt = Instant.now();

    @Column(name = "version")
    @Version
    private int version = 1;

    public Post(UserInfo userInfo, String title, String content, String image){
        this.userInfo = userInfo;
        this.title = title;
        this.content = content;
        this.image = image;
        deletedAt = null;
        editedAt = null;
        this.postState = new PostState(this);
    }


    public void update(String title, String content, String image){
        this.title = title;
        this.content = content;
        this.image = image;
        this.editedAt = Instant.now();
    }
    public String getMaskedTitle(){
        return postState.isBlind() ? "신고 처리된 글" : title;
    }

    public int getCommentCount(){
        return postState.getCommentCount();
    }
    public void view(){ postState.view();}
    public int like(){
        return postState.like();
    }
    public int unlike(){
        return postState.unlike();
    }
    public void addComment(){
        postState.addComment();
    }
    public void deleteComment() {postState.deleteComment();}
    public int report(){
        return postState.report();
    }

    public boolean isBlind(){
        return postState.isBlind();
    }
    public void delete(){
        deletedAt = Instant.now();
    }

}
