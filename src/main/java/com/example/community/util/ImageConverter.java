package com.example.community.util;

import com.example.community.handler.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImageConverter {

    private static final String POST_PREFIX = "posts/";
    private static final String PROFILE_PREFIX = "profiles/";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String updatePostImage(MultipartFile file){
        return updateImage(file, POST_PREFIX);
    }

    public String updateProfileImage(MultipartFile file){
        return updateImage(file, PROFILE_PREFIX);
    }

    private String updateImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = prefix + UUID.randomUUID() + "." + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        } catch (IOException exception) {
            throw new ImageUploadException("이미지 파일을 읽을 수 없습니다.", exception);
        }

        return objectKey;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 비어 있습니다.");
        }

        int dotIndex = originalFilename.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }
}