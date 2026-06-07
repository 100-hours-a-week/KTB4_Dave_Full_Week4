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
    private int view = 0;
    private int like = 0;
    private int report = 0;
    private int numberOfComments = 0;
    private boolean edited = false;
    private boolean deleted = false;
    private LocalDateTime saveTime = LocalDateTime.now();
    private LocalDateTime writeTime = LocalDateTime.now();

    public void update(Post post){
        this.title = post.getTitle();
        this.content = post.getContent();
        this.image = post.getImage();
        this.edited = true;
        this.saveTime = LocalDateTime.now();
    }

    public void update(String title, String content, String image){
        this.title = title;
        this.content = content;
        this.image = image;
        this.edited = true;
        this.saveTime = LocalDateTime.now();
    }

    public void delete(){
        this.deleted = true;
    }

    public void like(){
        like = like +1;
    }
    public void unlike(){
        like = like -1;
    }

    public void report(){
        report = report + 1;
    }

    public void addComment(){
        numberOfComments = numberOfComments + 1;
    }
}
