package com.example.community.post.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record PostRequest(
        @NotBlank
        @Size(max=26)
        String title,
        @NotBlank
        String content,
        @Min(1)
        Long temporaryPostId,
        @Size(max = 255)
        String objectKey,
        MultipartFile image
) {
    @AssertTrue(message = "objectKey와 이미지 파일을 동시에 보낼 수 없습니다.")
    public boolean isImageRequestValid() {
        boolean hasObjectKey = objectKey != null && !objectKey.isBlank();
        boolean hasImage = image != null && !image.isEmpty();
        return !(hasObjectKey && hasImage);
    }

    @AssertTrue(message = "objectKey를 사용할 때 temporaryPostId가 필요합니다.")
    public boolean isObjectKeySourceValid() {
        boolean hasObjectKey = objectKey != null && !objectKey.isBlank();
        return !hasObjectKey || temporaryPostId != null;
    }
}
