package com.example.community.user.service;

import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserAvailabilityQueryServiceTest {
    @Mock
    private SignInfoRepository signInfoRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @InjectMocks
    private UserAvailabilityQueryService availabilityQueryService;

    private String email;
    private String nickname;

    @BeforeEach
    void init(){
        email = "wns1628@gmail.com";
        nickname = "dave";
    }

    @Test
    @DisplayName("이메일 사용 여부 조회는 로그인 저장소 결과를 반환한다")
    void emailAvailabilityUsesSignInfoRepository() {
        when(signInfoRepository.existsByEmail(email)).thenReturn(true);

        assertThat(availabilityQueryService.isExistEmail(email)).isTrue();

        verify(signInfoRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("닉네임 사용 여부 조회는 프로필 저장소 결과를 반환한다")
    void nicknameAvailabilityUsesUserInfoRepository() {
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        assertThat(availabilityQueryService.isExistNickname(nickname))
                .isFalse();

        verify(userInfoRepository).existsByNickname(nickname);
    }


























}
