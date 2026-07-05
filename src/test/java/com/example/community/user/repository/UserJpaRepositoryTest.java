package com.example.community.user.repository;

import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
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
    private SignInfoJpaRepository signInfoJpaRepository;

    @Autowired
    private UserInfoJpaRepository userInfoJpaRepository;

    private UserDTO userDTO;

    @BeforeEach
    void init() {
        userInfoJpaRepository.deleteAll();
        signInfoJpaRepository.deleteAll();
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
                signInfoJpaRepository.findByUserNum(userNum);

        List<UserInfo> userInfos =
                userInfoJpaRepository.findBySignInfo_UserNum(userNum);

        List<UserInfo> savedUserInfos =
                userInfoJpaRepository.findBySignInfo_UserNum(userNum);

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
        signInfoJpaRepository.deleteAll();
        assertThat(userJpaRepository.getCountUser()).isEqualTo(0);
    }

    @Test
    @DisplayName("유저 번호로 유저 정보 조회")
    void findByUserNum(){
        UserDTO user = userJpaRepository.addUser(userDTO);
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
}