package com.example.community.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ImageUrlBuilder {
    private final String baseUrl;

    public ImageUrlBuilder(@Value("${aws.s3.base-url}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("S3 기본 URL이 비어 있습니다.");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public String build(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return baseUrl + objectKey;
    }
}
