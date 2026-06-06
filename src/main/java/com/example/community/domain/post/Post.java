package com.example.community.domain.post;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Post {
    private long postNum;
    private long userNum;
    private String title;
    private String content;
    private String image;
    private long views = 0;
    private long likes = 0;
    private long comments = 0;
    private long reports = 0;
    private boolean isEdited = false;
    private boolean isDeleted = false;
    private LocalDateTime saveTime = LocalDateTime.now();
    private LocalDateTime writeTime = LocalDateTime.now();

    public void update(Post post){
        this.title = post.getTitle();
        this.content = post.getContent();
        this.image = post.getImage();
        this.isEdited = true;
        this.saveTime = LocalDateTime.now();
    }

    public void delete(){
        this.isDeleted = true;
    }

    public void like(){
        likes = likes+1;
    }
    public void unlike(){
        likes = likes-1;
    }

    public void report(){
        reports = reports + 1;
    }

    public void addComment(){
        comments = comments + 1;
    }
}
