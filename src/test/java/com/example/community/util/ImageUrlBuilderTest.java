package com.example.community.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUrlBuilderTest {
    private static final String BASE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com";
    private final ImageUrlBuilder imageUrlBuilder =
            new ImageUrlBuilder(BASE_URL);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("objectKey가 비어 있으면 이미지 URL도 null이다")
    void buildReturnsNullForEmptyObjectKey(String objectKey) {
        assertThat(imageUrlBuilder.build(objectKey)).isNull();
    }

    @Test
    @DisplayName("objectKey가 있으면 S3 기본 URL을 결합한다")
    void buildReturnsS3UrlForObjectKey() {
        assertThat(imageUrlBuilder.build("posts/image.png"))
                .isEqualTo(
                        BASE_URL + "/posts/image.png"
                );
    }

    @Test
    @DisplayName("기본 URL 끝에 슬래시가 있어도 중복하지 않는다")
    void constructorKeepsSingleTrailingSlash() {
        ImageUrlBuilder builder = new ImageUrlBuilder(BASE_URL + "/");

        assertThat(builder.build("profiles/image.png"))
                .isEqualTo(BASE_URL + "/profiles/image.png");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("S3 기본 URL이 비어 있으면 생성을 거부한다")
    void constructorRejectsEmptyBaseUrl(String baseUrl) {
        assertThatThrownBy(() -> new ImageUrlBuilder(baseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("S3 기본 URL이 비어 있습니다.");
    }
}
