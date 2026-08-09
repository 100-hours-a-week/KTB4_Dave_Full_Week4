package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.request.TemporaryPostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUpdateResolver;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemporaryPostCommandService {
    private final TemporaryPostRepository temporaryPostRepository;
    private final UserInfoRepository userInfoRepository;
    private final TemporaryPostAccess temporaryPostAccess;
    private final ImageConverter imageConverter;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional
    public TemporaryKeyResponse createTemporaryPost(
            SignUserInfo signUserInfo,
            TemporaryPostRequest request
    ) {
        UserInfo userInfo = userInfoRepository
                .findByProfileId(signUserInfo.profileId())
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
        String image = imageConverter.updatePostImage(request.image());
        TemporaryPost temporaryPost = new TemporaryPost(userInfo);
        temporaryPost.update(request.title(), request.content(), image);
        temporaryPostRepository.save(temporaryPost);
        return new TemporaryKeyResponse(
                temporaryPost.getTemporaryId(),
                temporaryPost.getImage()
        );
    }

    @Transactional
    public TemporaryPostResponse updateTemporaryPost(
            SignUserInfo signUserInfo,
            long temporaryId,
            PostUpdateRequest request
    ) {
        TemporaryPost temporaryPost = temporaryPostAccess.findOwned(
                signUserInfo,
                temporaryId
        );
        String image = ImageUpdateResolver.resolve(
                temporaryPost.getImage(),
                request.objectKey(),
                request.image(),
                imageConverter::updatePostImage
        );
        temporaryPost.update(request.title(), request.content(), image);
        return TemporaryPostResponse.from(temporaryPost, imageUrlBuilder);
    }

    @Transactional
    public void deleteTemporaryPost(
            SignUserInfo signUserInfo,
            long temporaryId
    ) {
        TemporaryPost temporaryPost = temporaryPostAccess.findOwned(
                signUserInfo,
                temporaryId
        );
        temporaryPostRepository.delete(temporaryPost);
    }
}
