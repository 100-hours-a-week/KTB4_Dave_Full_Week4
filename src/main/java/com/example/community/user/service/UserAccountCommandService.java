package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.event.UserDisplayChangedEvent;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountCommandService {
    private final SignInfoRepository signInfoRepository;
    private final UserInfoRepository userInfoRepository;
    private final ImageConverter imageConverter;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (signInfoRepository.existsByEmail(request.email())) {
            throw new DuplicateException("중복 이메일 존재");
        }
        if (userInfoRepository.existsByNickname(request.nickname())) {
            throw new DuplicateException("중복 닉네임 존재");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        String image = imageConverter.updateProfileImage(request.imageFile());
        SignInfo signInfo = new SignInfo(
                request.email(),
                passwordEncoder.encode(request.password())
        );
        signInfoRepository.save(signInfo);
        userInfoRepository.save(new UserInfo(
                signInfo,
                request.nickname(),
                image
        ));
        return new SignUpResponse(signInfo.getUserNum());
    }

    @Transactional
    public void changePassword(
            SignUserInfo signUserInfo,
            PasswordChangeRequest request
    ) {
        SignInfo signInfo = signInfoRepository
                .findByUserNum(signUserInfo.userNum())
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
        if (!passwordEncoder.matches(
                request.password(),
                signInfo.getPassword()
        )) {
            throw new BadRequestException("비밀번호가 틀렸습니다.");
        }
        if (!request.nextPassword().equals(request.passwordConfirm())) {
            throw new BadRequestException("비밀번호 확인 불일치");
        }
        signInfo.changePassword(passwordEncoder.encode(
                request.nextPassword()
        ));
    }

    @Transactional
    public UserDeleteResponse deleteUser(SignUserInfo signUserInfo) {
        UserInfo userInfo = userInfoRepository
                .findByProfileId(signUserInfo.profileId())
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
        userInfo.delete();
        eventPublisher.publishEvent(new UserDisplayChangedEvent(
                signUserInfo.profileId()
        ));
        return new UserDeleteResponse(
                userInfo.getSignInfo().getUserNum(),
                userInfo.isDeleted()
        );
    }
}
