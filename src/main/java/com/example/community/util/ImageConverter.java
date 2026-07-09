package com.example.community.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class ImageConverter {
    private static final String POST_URL_PREFIX = "/images/posts/";
    private static final String PROFILE_URL_PREFIX = "/images/profiles/";

    public String updatePostImage(MultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get(System.getProperty("user.dir"), "app");
        Path targetPath = uploadPath.resolve(storedFileName);

        file.transferTo(targetPath);

        return POST_URL_PREFIX + storedFileName;
    }

    public String updateProfileImage(MultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get(System.getProperty("user.dir"), "app");
        Path targetPath = uploadPath.resolve(storedFileName);

        file.transferTo(targetPath);

        return PROFILE_URL_PREFIX + storedFileName;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 비어 있습니다.");
        }

        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }
}
