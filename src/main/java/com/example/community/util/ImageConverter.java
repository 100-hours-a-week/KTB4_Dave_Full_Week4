package com.example.community.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ImageConverter {

    private static final String POST_PREFIX = "posts/";
    private static final String PROFILE_PREFIX = "profiles/";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public String updatePostImage(MultipartFile file) throws IOException {
        return updateImage(file, POST_PREFIX);
    }

    public String updateProfileImage(MultipartFile file) throws IOException {
        return updateImage(file, PROFILE_PREFIX);
    }

    private String updateImage(
            MultipartFile file,
            String prefix
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;
        String objectKey = prefix + storedFileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()
                )
        );

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