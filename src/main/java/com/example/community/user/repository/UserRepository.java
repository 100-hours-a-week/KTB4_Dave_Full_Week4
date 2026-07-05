package com.example.community.user.repository;

import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.UserDTO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    UserDTO addUser(UserDTO user);
    long getCountUser();
    Optional<UserDTO> findByUserNum(long userNum);
    Optional<UserInfoDTO> findByProfileId(long profileId);
    Optional<UserDTO> findByEmail(String email);
    Optional<UserInfoDTO> findByNickname(String nickname);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoDTO updateUserInfo(long profileId, String nickname, String profileImage);
    void changePassword(long userNum, String nextPassword);
    Instant deleteUser(long profileId);
    Optional<UserInfoDTO> getUserInfo(long profileId);
    List<UserInfoDTO> getUserInfos(List<Long> profileIds);
}
