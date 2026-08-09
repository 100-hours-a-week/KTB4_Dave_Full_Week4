package com.example.community.temporaryPost.service;

import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemporaryPostQueryService {
    private final TemporaryPostRepository temporaryPostRepository;
    private final TemporaryPostAccess temporaryPostAccess;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional(readOnly = true)
    public TemporaryPostResponse getTemporaryPost(
            SignUserInfo signUserInfo,
            long temporaryId
    ) {
        TemporaryPost temporaryPost = temporaryPostAccess.findOwned(
                signUserInfo,
                temporaryId
        );
        return TemporaryPostResponse.from(temporaryPost, imageUrlBuilder);
    }

    @Transactional(readOnly = true)
    public List<TemporaryPostTitleResponse> getTemporaryPosts(
            SignUserInfo signUserInfo
    ) {
        return temporaryPostRepository
                .findByUserInfo_ProfileId(signUserInfo.profileId())
                .stream()
                .map(TemporaryPostTitleResponse::from)
                .toList();
    }
}
