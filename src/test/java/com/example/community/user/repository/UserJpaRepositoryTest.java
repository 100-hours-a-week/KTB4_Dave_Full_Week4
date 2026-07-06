package com.example.community.user.repository;

import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserJpaRepository.class)
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private UserDTO userDTO;

    @BeforeEach
    void init() {
        userInfoRepository.deleteAll();
        signInfoRepository.deleteAll();
        userDTO = UserDTO.of(
                new SignUpRequest(
                        "wns1628@gmail.com",
                        "1234",
                        "1234",
                        "dave",
                        null
                )
        );
    }


    @Test
    @DisplayName("유저 추가 시 SignInfo와 UserInfo로 분할 저장하고 userNum 반환")
    void addUser() {
        // when
        UserDTO user = userJpaRepository.addUser(userDTO);
        long userNum = user.getUserNum();
        // then
        assertThat(userNum).isPositive();

        Optional<SignInfo> signInfo =
                signInfoRepository.findByUserNum(userNum);

        List<UserInfo> userInfos =
                userInfoRepository.findBySignInfo_UserNum(userNum);

        List<UserInfo> savedUserInfos =
                userInfoRepository.findBySignInfo_UserNum(userNum);

        assertThat(signInfo).isPresent();
        assertThat(signInfo.get().getEmail()).isEqualTo(userDTO.getEmail());
        assertThat(signInfo.get().getPassword()).isEqualTo(userDTO.getPassword());

        assertThat(savedUserInfos).hasSize(1);

        UserInfo savedUserInfo = savedUserInfos.getFirst();
        assertThat(savedUserInfo.getNickname()).isEqualTo(userDTO.getNickname());
        assertThat(savedUserInfo.getProfileImage()).isEqualTo(userDTO.getProfileImage());
        assertThat(savedUserInfo.getSignInfo().getUserNum()).isEqualTo(userNum);
    }

    @Test
    @DisplayName("유저 수 조회")
    void getCountUser(){
        assertThat(userJpaRepository.getCountUser()).isEqualTo(0);
        userJpaRepository.addUser(userDTO);
        assertThat(userJpaRepository.getCountUser()).isEqualTo(1);
        signInfoRepository.deleteAll();
        assertThat(userJpaRepository.getCountUser()).isEqualTo(0);
    }

    @Test
    @DisplayName("유저 번호로 유저 정보 조회")
    void findByUserNum(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        Optional<UserDTO> empty = userJpaRepository.findByUserNum(0);
        assertThat(empty).isEmpty();
        long userNum = user.getUserNum();
        Optional<UserDTO> result = userJpaRepository.findByUserNum(userNum);

        assertThat(result).isPresent();
        UserDTO userResult = result.get();
        assertThat(userResult.equals(user)).isTrue();
    }

    @Test
    @DisplayName("프로필 아이디로 유저 정보 조회")
    void findByProfileId(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        Optional<UserInfoDTO> result = userJpaRepository.findByProfileId(user.getProfileId());

        assertThat(result).isPresent();
        UserInfoDTO userResult = result.get();
        assertThat(userResult.getProfileId()).isEqualTo(user.getProfileId());
        assertThat(userResult.getNickname()).isEqualTo(user.getNickname());
        assertThat(userResult.getUserNum()).isEqualTo(user.getUserNum());
        assertThat(userResult.getProfileImage()).isEqualTo(user.getProfileImage());
        assertThat(userResult.getUserRole()).isEqualTo(user.getUserRole());
        assertThat(userResult.getDeletedAt()).isEqualTo(user.getDeletedAt());
    }

    @Test
    @DisplayName("이메일로 유저 정보 조회")
    void findByEmail(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        Optional<UserDTO> signInfo = userJpaRepository.findByEmail(userDTO.getEmail());

        assertThat(signInfo).isPresent();
        UserDTO userResult = signInfo.get();
        assertThat(userResult.equals(user)).isTrue();
    }

    @Test
    @DisplayName("닉네임으로 유저 정보 조회")
    void findByNickname(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        Optional<UserInfoDTO> userInfoDTO = userJpaRepository.findByNickname(userDTO.getNickname());

        assertThat(userInfoDTO).isPresent();
        UserInfoDTO result = userInfoDTO.get();
        assertThat(result.getProfileId()).isEqualTo(user.getProfileId());
        assertThat(result.getNickname()).isEqualTo(user.getNickname());
        assertThat(result.getUserNum()).isEqualTo(user.getUserNum());
        assertThat(result.getProfileImage()).isEqualTo(user.getProfileImage());
        assertThat(result.getUserRole()).isEqualTo(user.getUserRole());
        assertThat(result.getDeletedAt()).isEqualTo(user.getDeletedAt());
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void isExistEmail(){
        UserDTO user = userJpaRepository.addUser(userDTO);

        assertThat(userJpaRepository.isExistEmail(user.getEmail())).isTrue();
        assertThat(userJpaRepository.isExistEmail("anoterEmail@test.com")).isFalse();
    }

    @Test
    @DisplayName("닉네임 중복 확인")
    void isExistNickname(){
        UserDTO user = userJpaRepository.addUser(userDTO);

        assertThat(userJpaRepository.isExistNickname(user.getNickname())).isTrue();
        assertThat(userJpaRepository.isExistNickname("anoterNickName")).isFalse();
    }

    @Test
    @DisplayName("유저 프로필 업데이트")
    void updateUserInfo(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        UserInfoDTO result = userJpaRepository.updateUserInfo(user.getProfileId(), "another", "test.png");
        UserDTO updated = userJpaRepository.findByUserNum(user.getUserNum()).orElseThrow();

        assertThat(updated.getNickname()).isEqualTo(result.getNickname());
        assertThat(updated.getProfileImage()).isEqualTo(result.getProfileImage());
    }

    @Test
    @DisplayName("비밀번호 변경")
    void changePassword(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        userJpaRepository.changePassword(user.getUserNum(), "5678");
        user = userJpaRepository.findByUserNum(user.getUserNum()).orElseThrow();

        assertThat(user.getPassword()).isEqualTo("5678");
    }

    @Test
    @DisplayName("유저 탈퇴")
    void deleteUser(){
        UserDTO user = userJpaRepository.addUser(userDTO);
        assertThat(user.getDeletedAt()).isNull();

        userJpaRepository.deleteUser(user.getUserNum());
        user = userJpaRepository.findByUserNum(user.getUserNum()).orElseThrow();

        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("유저 번호로 유저 정보를 조회")
    void getUserInfo() {
        String email = "wns1628@gmail.com";
        String password = "1234";
        String nickname = "dave";
        String profileImage = null;

        SignInfo signInfo = new SignInfo(email, password);
        signInfoRepository.save(signInfo);

        UserInfo userInfo = new UserInfo(signInfo, nickname, profileImage);
        userInfoRepository.save(userInfo);

        Optional<UserInfoDTO> result = userJpaRepository.getUserInfo(signInfo.getUserNum());

        assertThat(result).isPresent();
        assertThat(result.get().getUserNum()).isEqualTo(signInfo.getUserNum());
        assertThat(result.get().getProfileId()).isEqualTo(userInfo.getProfileId());
        assertThat(result.get().getNickname()).isEqualTo(nickname);
        assertThat(result.get().getProfileImage()).isEqualTo(profileImage);
        assertThat(result.get().getUserRole()).isEqualTo(UserRole.USER);
        assertThat(result.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("프로필 번호 목록으로 유저 정보 목록을 조회")
    void getUserInfos() {
        SignInfo signInfo1 = new SignInfo("user1@example.com", "1234");
        SignInfo signInfo2 = new SignInfo("user2@example.com", "1234");
        SignInfo signInfo3 = new SignInfo("user3@example.com", "1234");

        signInfoRepository.save(signInfo1);
        signInfoRepository.save(signInfo2);
        signInfoRepository.save(signInfo3);

        UserInfo userInfo1 = new UserInfo(signInfo1, "dave1", null);
        UserInfo userInfo2 = new UserInfo(signInfo2, "dave2", null);
        UserInfo userInfo3 = new UserInfo(signInfo3, "dave3", null);

        userInfoRepository.save(userInfo1);
        userInfoRepository.save(userInfo2);
        userInfoRepository.save(userInfo3);

        List<Long> profileIds = List.of(
                userInfo1.getProfileId(),
                userInfo3.getProfileId()
        );

        List<UserInfoDTO> result = userJpaRepository.getUserInfos(profileIds);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(UserInfoDTO::getProfileId)
                .containsExactlyInAnyOrder(
                        userInfo1.getProfileId(),
                        userInfo3.getProfileId()
                );

        assertThat(result)
                .extracting(UserInfoDTO::getNickname)
                .containsExactlyInAnyOrder("dave1", "dave3");
    }
}