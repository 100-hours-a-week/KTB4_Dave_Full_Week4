package com.example.community.temporaryPost.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class TemporaryPostDTO {
    private UUID temporaryKey;
    private String title;
    private String content;
    private String image;
    private Instant writeAt;
}
