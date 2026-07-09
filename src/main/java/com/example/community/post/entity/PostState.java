package com.example.community.post.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PostStat")
public class PostState {
    @Id
    private Long postNum;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", unique = true)
    private Post post;

    @Column(name = "viewCount")
    private int viewCount = 0;
    @Column(name = "likeCount")
    private int likeCount = 0;
    @Column(name = "commentCount")
    private int commentCount = 0;

    @Column(name = "reportCount")
    private Integer reportCount = 0;

    public PostState(Post post){
        this.postNum = post.getPostNum();
        this.post = post;
    }

    public void view(){ viewCount = viewCount +1;}
    public int like(){
        return likeCount = likeCount +1;
    }
    public int unlike(){
        return likeCount = likeCount -1;
    }
    public void addComment(){
        commentCount = commentCount + 1;
    }
    public int report(){
        return reportCount = reportCount + 1;
    }
    public boolean isBlind(){
        return reportCount > 5;
    }
}
