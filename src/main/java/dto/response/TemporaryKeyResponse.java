package dto.response;

public record TemporaryKeyResponse(
        long userNum,
        String temporary_key
) {
}
