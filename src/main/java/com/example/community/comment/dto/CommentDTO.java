package com.example.community.comment.dto;

import com.example.community.comment.entity.Comment;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private long commentNum;
    private long postNum;
    private Long parentNum;
    private int depth;
    private long profileId;
    private String content;
    private Instant editedAt;
    private Instant deletedAt;
    private Instant writeAt = Instant.now();

    public CommentDTO(long postNum, Long parentNum, int depth, long profileId, String content){
        this.postNum = postNum;
        this.parentNum = parentNum;
        this.depth = depth;
        this.profileId = profileId;
        this.content = content;
    }
    public CommentDTO(long postNum, long profileId, String content){
        this.postNum = postNum;
        this.parentNum = null;
        this.depth = 0;
        this.profileId = profileId;
        this.content = content;
    }

    public static CommentDTO from(Comment comment){
        return new CommentDTO(comment.getCommentNum(),
                comment.getPost().getPostNum(),
                comment.getComment() != null ? comment.getComment().getCommentNum() : null,
                comment.getDepth(),
                comment.getUserInfo().getProfileId(),
                comment.getContent(), comment.getEditedAt(),
                comment.getDeletedAt(), comment.getWriteAt());
    }

    public void update(String content){
        this.content = content;
        editedAt = Instant.now();
        editedAt = Instant.now();
    }
    public boolean isDeleted(){
        return this.deletedAt != null;
    }

    public void delete(){
        this.deletedAt = Instant.now();
    }
}
