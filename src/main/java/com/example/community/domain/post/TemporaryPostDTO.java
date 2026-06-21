package com.example.community.domain.post;

import lombok.*;

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
