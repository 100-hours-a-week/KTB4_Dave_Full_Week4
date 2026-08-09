package com.example.community.user.service;

import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.event.UserDisplayChangedEvent;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUpdateResolver;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileCommandService {
    private final UserInfoRepository userInfoRepository;
    private final ImageConverter imageConverter;
    private final ImageUrlBuilder imageUrlBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserInfoResponse updateUserInfo(
            SignUserInfo signUserInfo,
            UserInfoRequest request
    ) {
        UserInfo userInfo = userInfoRepository
                .findByProfileId(signUserInfo.profileId())
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
        boolean duplicatedNickname = userInfoRepository.existsByNickname(
                request.nickname()
        );
        if (duplicatedNickname
                && !request.nickname().equals(userInfo.getNickname())) {
            throw new DuplicateException("중복 닉네임 존재");
        }

        String profileImage = ImageUpdateResolver.resolve(
                userInfo.getProfileImage(),
                request.objectKey(),
                request.imageFile(),
                imageConverter::updateProfileImage
        );
        userInfo.update(request.nickname(), profileImage);
        eventPublisher.publishEvent(new UserDisplayChangedEvent(
                signUserInfo.profileId()
        ));
        return UserInfoResponse.from(userInfo, imageUrlBuilder);
    }
}
