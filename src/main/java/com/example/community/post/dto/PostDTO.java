package com.example.community.post.dto;

import com.example.community.post.entity.Post;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long postNum;
    private Long profileId;
    private String title;
    private String content;
    private String image;
    private int viewCount = 0;
    private int likeCount = 0;
    private int reportCount = 0;
    private int commentCount = 0;
    private Instant deletedAt;
    private Instant editedAt;
    private Instant writeAt = Instant.now();
    private int version;

    public PostDTO(Long profileId, String title, String content, String image){
        this.profileId = profileId;
        this.title = title;
        this.content = content;
        this.image = image;
        deletedAt = null;
        editedAt = null;
        version = 1;
    }

    public static PostDTO from(Post post){
        return new PostDTO(post.getPostNum(), post.getUserInfo().getProfileId(),
                post.getTitle(), post.getContent(), post.getImage(), post.getViewCount(),
                post.getLikeCount(), post.getReportCount(), post.getCommentCount(),
                post.getDeletedAt(), post.getEditedAt(), post.getWriteAt(), post.getVersion());
    }

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
    public void view(){ viewCount = viewCount +1;}
    public void like(){
        likeCount = likeCount +1;
    }
    public void unlike(){
        likeCount = likeCount -1;
    }

    public void report(){
        reportCount = reportCount + 1;
    }

    public void addComment(){
        commentCount = commentCount + 1;
    }
}
