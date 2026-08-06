package com.example.community.util;

import com.example.community.handler.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ImageUpdateResolver {

    private ImageUpdateResolver() {
    }

    public static String resolve(
            String currentImage,
            String requestedObjectKey,
            MultipartFile image,
            Function<MultipartFile, String> imageUploader
    ) {
        return resolveInternal(
                requestedObjectKey,
                image,
                objectKey -> Objects.equals(currentImage, objectKey),
                imageUploader
        );
    }

    public static String resolveForCreate(
            String requestedObjectKey,
            MultipartFile image,
            Predicate<String> objectKeyValidator,
            Function<MultipartFile, String> imageUploader
    ) {
        return resolveInternal(
                requestedObjectKey,
                image,
                objectKeyValidator,
                imageUploader
        );
    }

    private static String resolveInternal(
            String requestedObjectKey,
            MultipartFile image,
            Predicate<String> objectKeyValidator,
            Function<MultipartFile, String> imageUploader
    ) {
        boolean hasObjectKey = hasText(requestedObjectKey);
        boolean hasImage = image != null && !image.isEmpty();
        validateExclusive(hasObjectKey, hasImage);

        if (hasImage) {
            return imageUploader.apply(image);
        }
        if (!hasObjectKey) {
            return null;
        }
        if (!objectKeyValidator.test(requestedObjectKey)) {
            throw new BadRequestException("유효하지 않은 objectKey입니다.");
        }
        return requestedObjectKey;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void validateExclusive(
            boolean hasObjectKey,
            boolean hasImage
    ) {
        if (hasObjectKey && hasImage) {
            throw new BadRequestException(
                    "objectKey와 이미지 파일을 동시에 보낼 수 없습니다."
            );
        }
    }
}
