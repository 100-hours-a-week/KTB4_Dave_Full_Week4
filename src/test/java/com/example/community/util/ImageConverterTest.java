package com.example.community.util;

import com.example.community.handler.exception.ImageUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageConverterTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    private ImageConverter imageConverter;

    @BeforeEach
    void setUp() {
        imageConverter = new ImageConverter(s3Client);
        ReflectionTestUtils.setField(imageConverter, "bucket", BUCKET);
    }

    @ParameterizedTest
    @CsvSource({
            "post, posts/",
            "profile, profiles/"
    })
    @DisplayName("이미지를 유형별 S3 경로에 저장하고 사용한 object key를 반환한다")
    void updateImageUploadsToExpectedPathAndReturnsObjectKey(
            String imageType,
            String expectedPrefix
    ) {
        MockMultipartFile file = imageFile("archive.image.PNG");
        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        String objectKey = switch (imageType) {
            case "post" -> imageConverter.updatePostImage(file);
            case "profile" -> imageConverter.updateProfileImage(file);
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 이미지 유형입니다."
            );
        };

        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(objectKey);
        assertThat(request.key()).startsWith(expectedPrefix);
        assertThat(request.key()).endsWith(".png");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(file.getSize());
    }

    @Test
    @DisplayName("파일이 null이거나 비어 있으면 null을 반환하고 S3를 호출하지 않는다")
    void updateImageReturnsNullWhenFileIsNullOrEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThat(imageConverter.updatePostImage(null)).isNull();
        assertThat(imageConverter.updateProfileImage(emptyFile)).isNull();
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("원본 파일명이 null이면 예외가 발생하고 S3를 호출하지 않는다")
    void updateImageRejectsNullOriginalFilename() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> imageConverter.updatePostImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 비어 있습니다.");
        verifyNoInteractions(s3Client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    @DisplayName("원본 파일명이 빈 문자열이나 공백이면 예외가 발생하고 S3를 호출하지 않는다")
    void updateImageRejectsBlankOriginalFilename(String originalFilename) {
        MockMultipartFile file = imageFile(originalFilename);

        assertThatThrownBy(() -> imageConverter.updatePostImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 비어 있습니다.");
        verifyNoInteractions(s3Client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image", "image."})
    @DisplayName("확장자가 없는 파일명이면 예외가 발생하고 S3를 호출하지 않는다")
    void updateImageRejectsFilenameWithoutExtension(String originalFilename) {
        MockMultipartFile file = imageFile(originalFilename);

        assertThatThrownBy(() -> imageConverter.updatePostImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일 확장자가 없습니다.");
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("이미지 파일을 읽을 수 없으면 업로드 예외로 변환하고 S3를 호출하지 않는다")
    void updateImageWrapsIOExceptionAndDoesNotCallS3() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        IOException cause = new IOException("read failure");
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenThrow(cause);

        assertThatThrownBy(() -> imageConverter.updatePostImage(file))
                .isInstanceOf(ImageUploadException.class)
                .hasMessage("이미지 파일을 읽을 수 없습니다.")
                .hasCause(cause);
        verifyNoInteractions(s3Client);
    }

    private MockMultipartFile imageFile(String originalFilename) {
        return new MockMultipartFile(
                "image",
                originalFilename,
                "image/png",
                "image-content".getBytes(UTF_8)
        );
    }
}
