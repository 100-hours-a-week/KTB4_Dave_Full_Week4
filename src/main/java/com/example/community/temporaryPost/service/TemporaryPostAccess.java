package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporaryPostAccess {
    private final TemporaryPostRepository temporaryPostRepository;

    public TemporaryPost findOwned(
            SignUserInfo signUserInfo,
            long temporaryId
    ) {
        TemporaryPost temporaryPost = temporaryPostRepository
                .findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException(
                        "존재하지 않는 임시저장글"
                ));
        if (!temporaryPost.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId())) {
            throw new ForbiddenException("접근권한 부족");
        }
        return temporaryPost;
    }
}
