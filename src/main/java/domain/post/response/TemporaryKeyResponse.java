package domain.post.response;

public record TemporaryKeyResponse(
        long userNum,
        String temporaryKey
) {
}
