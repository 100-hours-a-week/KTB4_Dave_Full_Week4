package com.example.community.domain.post.response;

import java.util.UUID;

public record TemporaryKeyResponse(
        long userNum,
        UUID temporaryKey
) {
}
