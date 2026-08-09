package com.example.community.user.service;

import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAvailabilityQueryService {
    private final SignInfoRepository signInfoRepository;
    private final UserInfoRepository userInfoRepository;

    @Transactional(readOnly = true)
    public boolean isExistEmail(String email) {
        return signInfoRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isExistNickname(String nickname) {
        return userInfoRepository.existsByNickname(nickname);
    }
}
