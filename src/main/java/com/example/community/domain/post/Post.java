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
    private LocalDateTime saveTime;
    private LocalDateTime writeTime;
}
