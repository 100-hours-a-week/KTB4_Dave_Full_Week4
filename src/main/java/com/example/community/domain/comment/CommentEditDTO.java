package com.example.community.domain.comment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentEditDTO {
    private long commentNum;
    private int version;
    private String content;
    private Instant writeAt;
}
