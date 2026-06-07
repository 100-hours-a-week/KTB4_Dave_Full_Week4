package com.example.community.domain.post;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class TemporaryPost {
    private UUID temporaryKey;
    private String title;
    private String content;
    private String image;
    private LocalDateTime saveTime;
}
