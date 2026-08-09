package com.example.community.util;

import com.example.community.handler.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ImageUpdateResolverTest {

    @ParameterizedTest(name = "mode={0}")
    @EnumSource(ResolveMode.class)
    @DisplayName("새 파일이 있으면 업로드 결과를 반환한다")
    void returnsUploadedImage(ResolveMode mode) {
        MultipartFile image = nonEmptyImage();
        Predicate<String> objectKeyValidator = validator();
        Function<MultipartFile, String> imageUploader = uploader();
        when(imageUploader.apply(image)).thenReturn("posts/new.png");

        String result = resolve(
                mode,
                null,
                image,
                objectKeyValidator,
                imageUploader
        );

        assertThat(result).isEqualTo("posts/new.png");
        verify(imageUploader).apply(image);
        verifyNoInteractions(objectKeyValidator);
    }

    @ParameterizedTest(name = "mode={0}")
    @EnumSource(ResolveMode.class)
    @DisplayName("objectKey와 파일이 없으면 null을 반환한다")
    void returnsNullWithoutObjectKeyAndImage(ResolveMode mode) {
        Predicate<String> objectKeyValidator = validator();
        Function<MultipartFile, String> imageUploader = uploader();

        String result = resolve(
                mode,
                null,
                null,
                objectKeyValidator,
                imageUploader
        );

        assertThat(result).isNull();
        verifyNoInteractions(objectKeyValidator, imageUploader);
    }

    @Test
    @DisplayName("공백 objectKey와 빈 파일은 이미지가 없는 요청으로 처리한다")
    void resolveTreatsBlankObjectKeyAndEmptyImageAsAbsent() {
        MultipartFile emptyImage = mock(MultipartFile.class);
        Function<MultipartFile, String> imageUploader = uploader();
        when(emptyImage.isEmpty()).thenReturn(true);

        String result = ImageUpdateResolver.resolve(
                "posts/current.png",
                " ",
                emptyImage,
                imageUploader
        );

        assertThat(result).isNull();
        verifyNoInteractions(imageUploader);
    }

    @Test
    @DisplayName("현재 이미지와 같은 objectKey이면 기존 이미지를 유지한다")
    void resolveKeepsCurrentImageForMatchingObjectKey() {
        Function<MultipartFile, String> imageUploader = uploader();

        String result = ImageUpdateResolver.resolve(
                "posts/current.png",
                "posts/current.png",
                null,
                imageUploader
        );

        assertThat(result).isEqualTo("posts/current.png");
        verifyNoInteractions(imageUploader);
    }

    @Test
    @DisplayName("현재 이미지와 다른 objectKey이면 수정 요청을 거부한다")
    void resolveRejectsMismatchedObjectKey() {
        Function<MultipartFile, String> imageUploader = uploader();

        assertThatThrownBy(() -> ImageUpdateResolver.resolve(
                "posts/current.png",
                "posts/other.png",
                null,
                imageUploader
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        verifyNoInteractions(imageUploader);
    }

    @ParameterizedTest(name = "mode={0}")
    @EnumSource(ResolveMode.class)
    @DisplayName("objectKey와 파일을 함께 전달하면 거부한다")
    void rejectsObjectKeyAndImageTogether(ResolveMode mode) {
        MultipartFile image = nonEmptyImage();
        Predicate<String> objectKeyValidator = validator();
        Function<MultipartFile, String> imageUploader = uploader();

        assertThatThrownBy(() -> resolve(
                mode,
                "posts/current.png",
                image,
                objectKeyValidator,
                imageUploader
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("objectKey와 이미지 파일을 동시에 보낼 수 없습니다.");

        verifyNoInteractions(objectKeyValidator, imageUploader);
    }

    @Test
    @DisplayName("검증된 objectKey이면 이미지 생성에 재사용한다")
    void resolveForCreateReturnsValidatedObjectKey() {
        Predicate<String> objectKeyValidator = validator();
        Function<MultipartFile, String> imageUploader = uploader();
        when(objectKeyValidator.test("posts/temporary.png"))
                .thenReturn(true);

        String result = ImageUpdateResolver.resolveForCreate(
                "posts/temporary.png",
                null,
                objectKeyValidator,
                imageUploader
        );

        assertThat(result).isEqualTo("posts/temporary.png");
        verify(objectKeyValidator).test("posts/temporary.png");
        verifyNoInteractions(imageUploader);
    }

    @Test
    @DisplayName("검증되지 않은 objectKey이면 이미지 생성을 거부한다")
    void resolveForCreateRejectsInvalidObjectKey() {
        Predicate<String> objectKeyValidator = validator();
        Function<MultipartFile, String> imageUploader = uploader();
        when(objectKeyValidator.test("posts/unverified.png"))
                .thenReturn(false);

        assertThatThrownBy(() -> ImageUpdateResolver.resolveForCreate(
                "posts/unverified.png",
                null,
                objectKeyValidator,
                imageUploader
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        verify(objectKeyValidator).test("posts/unverified.png");
        verifyNoInteractions(imageUploader);
    }

    @SuppressWarnings("unchecked")
    private Function<MultipartFile, String> uploader() {
        return mock(Function.class);
    }

    @SuppressWarnings("unchecked")
    private Predicate<String> validator() {
        return mock(Predicate.class);
    }

    private MultipartFile nonEmptyImage() {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        return image;
    }

    private String resolve(
            ResolveMode mode,
            String requestedObjectKey,
            MultipartFile image,
            Predicate<String> objectKeyValidator,
            Function<MultipartFile, String> imageUploader
    ) {
        return switch (mode) {
            case UPDATE -> ImageUpdateResolver.resolve(
                    "posts/current.png",
                    requestedObjectKey,
                    image,
                    imageUploader
            );
            case CREATE -> ImageUpdateResolver.resolveForCreate(
                    requestedObjectKey,
                    image,
                    objectKeyValidator,
                    imageUploader
            );
        };
    }

    private enum ResolveMode {
        UPDATE,
        CREATE
    }
}
