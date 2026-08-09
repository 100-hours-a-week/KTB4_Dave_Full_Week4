package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.event.UserDisplayChangedEvent;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserProfileCommandServiceTest {
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private ImageConverter imageConverter;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );
    @InjectMocks
    private UserProfileCommandService profileCommandService;












    @Test
    @DisplayName("유저 정보 수정 시 존재하지 않는 프로필 번호 입력 시 실패")
    void updateUserInfoFailsWhenProfileDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.empty());
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                "dave2",
                null,
                null
        );

        assertThatThrownBy(() -> profileCommandService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("유저 정보 수정 시 중복 닉네임 입력 시 실패")
    void updateUserInfoFailsWhenNicknameIsDuplicated() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", null)));
        when(userInfoRepository.existsByNickname(any(String.class))).thenReturn(true);
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                "dave2",
                null,
                null
        );

        assertThatThrownBy(() -> profileCommandService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("유저 정보 수정 시 닉네임 변경하지 않고 프로필 이미지만 변경 시 중복 처리 통과")
    void updateUserInfoSucceedsWhenOnlyProfileImageIsChanged() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", "/temp");
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname(userInfo.getNickname())).thenReturn(true);
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                "dave",
                null,
                null
        );
        assertThat(profileCommandService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(userInfo, imageUrlBuilder));
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("현재 objectKey를 전달하면 기존 프로필 이미지를 유지한다")
    void updateUserInfoKeepsExistingProfileImage() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(
                new SignInfo("wns1628@gmail.com", "1234"),
                "dave",
                "profiles/old.png"
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname("dave2")).thenReturn(false);
        UserInfoRequest request = new UserInfoRequest(
                "dave2",
                "profiles/old.png",
                null
        );

        UserInfoResponse response = profileCommandService.updateUserInfo(
                signUserInfo,
                request
        );

        assertThat(response.profileImage()).endsWith("profiles/old.png");
        assertThat(response.objectKey()).isEqualTo("profiles/old.png");
        assertThat(userInfo.getProfileImage()).isEqualTo("profiles/old.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("회원정보 수정 시 현재 값과 다른 objectKey는 거부한다")
    void updateUserInfoRejectsMismatchedObjectKey() {
        SignUserInfo signUserInfo =
                new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(
                new SignInfo("wns1628@gmail.com", "1234"),
                "dave",
                "profiles/old.png"
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname("dave2")).thenReturn(false);
        UserInfoRequest request = new UserInfoRequest(
                "dave2",
                "profiles/other.png",
                null
        );

        assertThatThrownBy(
                () -> profileCommandService.updateUserInfo(signUserInfo, request)
        ).isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        assertThat(userInfo.getProfileImage()).isEqualTo("profiles/old.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("objectKey 없이 빈 이미지가 오면 프로필 이미지를 삭제한다")
    void updateUserInfoDeletesProfileImageForEmptyFile() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(
                new SignInfo("wns1628@gmail.com", "1234"),
                "dave",
                "profiles/old.png"
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname("dave2")).thenReturn(false);
        MockMultipartFile emptyImage = new MockMultipartFile(
                "imageFile",
                "",
                "application/octet-stream",
                new byte[0]
        );
        UserInfoRequest request = new UserInfoRequest(
                "dave2",
                null,
                emptyImage
        );

        UserInfoResponse response = profileCommandService.updateUserInfo(
                signUserInfo,
                request
        );

        assertThat(response.profileImage()).isNull();
        assertThat(userInfo.getProfileImage()).isNull();
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("이미지 없는 유저 정보 수정 성공")
    void updateUserInfoSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", null);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname(any(String.class))).thenReturn(false);
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                "dave2",
                null,
                null
        );

        assertThat(profileCommandService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(userInfo, imageUrlBuilder));
        verify(eventPublisher).publishEvent(new UserDisplayChangedEvent(1L));
    }

    @Test
    @DisplayName("이미지 있는 유저 정보 수정 성공")
    void updateUserInfoWithImageSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        UserInfo userInfo = new UserInfo(new SignInfo("wns1628@gmail.com","1234"), "dave", null);
        when(userInfoRepository.findByProfileId(1L)).thenReturn(Optional.of(userInfo));
        when(userInfoRepository.existsByNickname(any(String.class))).thenReturn(false);
        MockMultipartFile image = new MockMultipartFile(
                "imageFile",           // 요청 필드명
                "profile.png",         // 원본 파일명
                "image/png",           // Content-Type
                "dummy-image".getBytes(UTF_8)
        );
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                "dave2",
                null,
                image
        );
        when(imageConverter.updateProfileImage(image))
                .thenReturn("profiles/new.png");

        assertThat(profileCommandService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(userInfo, imageUrlBuilder));
    }









}
