package com.example.community.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UserInfoRequest(
        @NotBlank
        @Size(max=10)
        String nickname,
        @Size(max = 255)
        String objectKey,
        MultipartFile imageFile
) {
    @AssertTrue(message = "objectKey와 이미지 파일을 동시에 보낼 수 없습니다.")
    public boolean isImageRequestValid() {
        boolean hasObjectKey = objectKey != null && !objectKey.isBlank();
        boolean hasImage = imageFile != null && !imageFile.isEmpty();
        return !(hasObjectKey && hasImage);
    }
}
