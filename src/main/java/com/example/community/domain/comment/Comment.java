package com.example.community.domain.comment;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Comment {
    private long commentNum;
    private long postNum;
    private long parentNum;
    private int depth;
    private long userNum;
    private String content;
    private boolean edited = false;
    private boolean deleted = false;
    private LocalDateTime saveTime = LocalDateTime.now();
    private LocalDateTime writeTime = LocalDateTime.now();

    public void update(String content){
        this.content = content;
        edited = true;
        saveTime = LocalDateTime.now();
    }

    public void delete(){
        this.deleted = true;
    }
}
