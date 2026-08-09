package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.post.entity.Post;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUpdateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PostImageResolver {
    private final TemporaryPostRepository temporaryPostRepository;
    private final ImageConverter imageConverter;

    String resolveForCreate(
            SignUserInfo signUserInfo,
            PostRequest request
    ) {
        TemporaryPost temporaryPost = findTemporaryPostForPublish(
                signUserInfo,
                request.temporaryPostId()
        );
        return ImageUpdateResolver.resolveForCreate(
                request.objectKey(),
                request.image(),
                objectKey -> temporaryPost != null
                        && Objects.equals(temporaryPost.getImage(), objectKey),
                imageConverter::updatePostImage
        );
    }

    String resolveForUpdate(Post post, PostUpdateRequest request) {
        return ImageUpdateResolver.resolve(
                post.getImage(),
                request.objectKey(),
                request.image(),
                imageConverter::updatePostImage
        );
    }

    void deleteReplacedImageAfterCommit(
            String previousObjectKey,
            String currentObjectKey
    ) {
        if (previousObjectKey != null
                && !Objects.equals(previousObjectKey, currentObjectKey)) {
            imageConverter.deleteAfterCommit(previousObjectKey);
        }
    }

    private TemporaryPost findTemporaryPostForPublish(
            SignUserInfo signUserInfo,
            Long temporaryPostId
    ) {
        if (temporaryPostId == null) {
            return null;
        }
        return temporaryPostRepository
                .findByTemporaryIdAndUserInfo_ProfileId(
                        temporaryPostId,
                        signUserInfo.profileId()
                )
                .orElseThrow(() -> new BadRequestException(
                        "유효하지 않은 임시저장글입니다."
                ));
    }
}
