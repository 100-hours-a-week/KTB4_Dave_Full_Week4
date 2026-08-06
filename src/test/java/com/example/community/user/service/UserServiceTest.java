package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private SignInfoRepository signInfoRepository;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private UserLikeRepository userLikeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ImageConverter imageConverter;
    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );
    @InjectMocks
    private UserService userService;

    private String email;
    private String encodedPassword;
    private String nickname;
    private String password;

    @BeforeEach
    void init(){
        email = "wns1628@gmail.com";
        password = "1234";
        encodedPassword = "encoded_password";
        nickname = "dave";
    }

    @Test
    @DisplayName("회원 가입 시 중복 이메일 입력 시 예외 발생")
    void signUpFailsWithDuplicateEmail() {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 이메일 존재");
    }


    @Test
    @DisplayName("회원 가입 시 중복 닉네임 입력 시 예외 발생")
    void signUpFailsWithDuplicateNickname() {
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(DuplicateException.class)
                .hasMessage("중복 닉네임 존재");
    }

    @Test
    @DisplayName("회원 가입 시 중복 비밀번호 확인 불일치 시 예외 발생")
    void signUpFailsWithPasswordConfirmMismatch() {
        String passwordConfirm = "12345";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        assertThatThrownBy(() -> userService.signUp(signUpRequest)).isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("회원 가입 성공")
    void signUpSuccess(){
        String passwordConfirm = "1234";
        SignUpRequest signUpRequest = new SignUpRequest(email, password, passwordConfirm, nickname, null);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(signInfoRepository.existsByEmail(email)).thenReturn(false);
        when(userInfoRepository.existsByNickname(nickname)).thenReturn(false);

        when(signInfoRepository.save(any(SignInfo.class)))
                .thenAnswer(invocation -> {
                    SignInfo signInfo = invocation.getArgument(0);

                    ReflectionTestUtils.setField(signInfo, "userNum", 1L);

                    return signInfo;
                });

        when(userInfoRepository.save(any(UserInfo.class)))
                .thenAnswer(invocation -> {
                    UserInfo userInfo = invocation.getArgument(0);

                    ReflectionTestUtils.setField(userInfo, "profileId", 1L);

                    return userInfo;
                });
        when(imageConverter.updateProfileImage(null)).thenReturn(null);
        assertThat(userService.signUp(signUpRequest).userId()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 이메일 입력 시 로그인 실패")
    void signInFailsWhenEmailDoesNotExist() {
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, password))).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 이메일");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 로그인 실패")
    void signInFailsWhenPasswordDoesNotMatch() {
        String wrongPassword = "123";
        SignInfo signInfo = new SignInfo(1L, email, encodedPassword, null, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, wrongPassword))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인 실패");
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인 시도 시 로그인 실패")
    void signInFailsWhenUserIsDeleted() {
        Instant now = Instant.now();
        SignInfo signInfo = new SignInfo(1L, email, password, now, null);

        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        assertThatThrownBy(() -> userService.signIn(new SignInRequest(email, password))).isInstanceOf(UnAuthorizedException.class)
                .hasMessage("탈퇴한 유저");
    }

    @Test
    @DisplayName("로그인 성공")
    void signInSuccess() {
        SignInfo signInfo = new SignInfo(1L, email, encodedPassword, null, null);
        UserInfo userInfo = new UserInfo(1L, signInfo,nickname, null, UserRole.USER, null);
        when(signInfoRepository.findByEmail(email)).thenReturn(Optional.of(signInfo));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(userInfoRepository.findBySignInfo_UserNum(1L)).thenReturn(Collections.singletonList(userInfo));
        UserInfoDTO userInfoDTO = UserInfoDTO.from(userInfo);
        userInfoDTO.setEmail(email);
        assertThat(userService.signIn(new SignInRequest(email, password)))
                .usingRecursiveComparison()
                .isEqualTo(userInfoDTO);
    }

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

        assertThatThrownBy(() -> userService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(NotFoundException.class)
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

        assertThatThrownBy(() -> userService.updateUserInfo(signUserInfo, userInfoRequest)).isInstanceOf(DuplicateException.class)
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
        assertThat(userService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
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

        UserInfoResponse response = userService.updateUserInfo(
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
                () -> userService.updateUserInfo(signUserInfo, request)
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

        UserInfoResponse response = userService.updateUserInfo(
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

        assertThat(userService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(userInfo, imageUrlBuilder));
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

        assertThat(userService.updateUserInfo(signUserInfo, userInfoRequest)).usingRecursiveComparison()
                .isEqualTo(UserInfoResponse.from(userInfo, imageUrlBuilder));
    }

    @Test
    @DisplayName("비밀번호 변경 시 존재하지 않는 유저이면 실패")
    void changePasswordFailsWhenUserDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("1234", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호가 일치하지 않으면 실패")
    void changePasswordFailsWhenCurrentPasswordDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest("wrong-password", "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(new SignInfo("wns1628@gmail.com", "1234")));

        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호가 틀렸습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호 확인이 일치하지 않으면 실패")
    void changePasswordFailsWhenNextPasswordConfirmDoesNotMatch() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest(password, "12345", "123456");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(new SignInfo("wns1628@gmail.com", encodedPassword)));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);


        assertThatThrownBy(() -> userService.changePassword(signUserInfo, passwordChangeRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("비밀번호 확인 불일치");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePasswordSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", encodedPassword);

        PasswordChangeRequest passwordChangeRequest =
                new PasswordChangeRequest(password, "12345", "12345");

        when(signInfoRepository.findByUserNum(signUserInfo.userNum()))
                .thenReturn(Optional.of(signInfo));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(passwordEncoder.encode(passwordChangeRequest.nextPassword())).thenReturn("encodedPassword2");

        System.out.println(signInfo.getPassword());
        userService.changePassword(signUserInfo, passwordChangeRequest);
        assertThat(signInfo.getPassword()).isEqualTo("encodedPassword2");
    }

    @ParameterizedTest
    @CsvSource({
            "likes, post.postState.likeCount",
            "views, post.postState.viewCount"
    })
    @DisplayName("좋아요한 게시글은 요청한 기준과 게시글 번호 역순으로 정렬한다")
    void getMyLikePostsUsesRequestedSortAndPostNumAsTieBreaker(
            String sort,
            String primarySortProperty
    ) {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userLikeRepository.findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        userService.getMyLikePosts(signUserInfo, 0, 10, sort);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userLikeRepository).findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders = pageable.getSort().stream().toList();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo(primarySortProperty);
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("post.postNum");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("좋아요한 게시글은 기본적으로 게시글 번호 역순으로 정렬한다")
    void getMyLikePostsUsesPostNumDescendingAsDefaultSort() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userLikeRepository.findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        userService.getMyLikePosts(signUserInfo, 0, 10, "latest");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userLikeRepository).findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getSort().stream().toList())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getProperty()).isEqualTo("post.postNum");
                    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
                });
    }

    @Test
    @DisplayName("좋아요한 게시글이 없으면 첫 페이지에 빈 결과를 반환한다")
    void getMyLikePostsReturnsEmptyFirstPage() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        Pageable pageable = PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "post.postNum")
        );
        when(userLikeRepository.findByUserInfo_ProfileId(
                signUserInfo.profileId(),
                pageable
        )).thenReturn(Page.empty(pageable));

        PostPageResponse response = userService.getMyLikePosts(
                signUserInfo,
                0,
                1,
                "latest"
        );

        assertThat(response.postTitleResponses()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.pageSize()).isEqualTo(1);
        assertThat(response.postCount()).isZero();
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPage()).isZero();
    }

    @Test
    @DisplayName("유저 삭제 시 존재하지 않는 프로필 번호이면 실패")
    void deleteUserFailsWhenProfileIdDoesNotExist() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userInfoRepository.findByProfileId(signUserInfo.profileId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(signUserInfo))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("유저 삭제 성공")
    void deleteUserSuccess() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);

        UserDeleteResponse userDeleteResponse =
                new UserDeleteResponse(signUserInfo.userNum(), true);
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", "1234");
        signInfo.setUserNum(signUserInfo.userNum());
        UserInfo userInfo = new UserInfo(signInfo, "dave", null);
        when(userInfoRepository.findByProfileId(signUserInfo.profileId()))
                .thenReturn(Optional.of(userInfo));

        assertThat(userService.deleteUser(signUserInfo))
                .usingRecursiveComparison()
                .isEqualTo(userDeleteResponse);
        assertThat(userInfo.isDeleted()).isTrue();
        assertThat(signInfo.isDeleted()).isTrue();
        verifyNoInteractions(signInfoRepository);
    }
}
