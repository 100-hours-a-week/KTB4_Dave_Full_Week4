package com.example.community.post.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record PostUpdateRequest(
        @NotBlank
        @Size(max = 26)
        String title,
        @NotBlank
        String content,
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
}
