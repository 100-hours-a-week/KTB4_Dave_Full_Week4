package com.example.community.domain.post;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class PostDTO {
    private long postNum;
    private long userNum;
    private String title;
    private String content;
    private String image;
    private int view = 0;
    private int like = 0;
    private int report = 0;
    private int numberOfComments = 0;
    private Instant deletedAt;
    private Instant editedAt;
    private Instant writeTime = Instant.now();

    public void update(PostDTO post){
        this.title = post.getTitle();
        this.content = post.getContent();
        this.image = post.getImage();
        this.editedAt = Instant.now();
    }

    public void update(String title, String content, String image){
        this.title = title;
        this.content = content;
        this.image = image;
        this.editedAt = Instant.now();
    }

    public boolean isDeleted(){
        return this.deletedAt != null;
    }
    public void delete(){
        deletedAt = Instant.now();
    }
    public void view(){ view = view+1;}
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
