package com.example.community.domain.comment;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentDTO {
    private long commentNum;
    private long postNum;
    private long parentNum;
    private int depth;
    private long userNum;
    private String content;
    private Instant editedAt;
    private Instant deletedAt;
    private Instant writeAt = Instant.now();

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
