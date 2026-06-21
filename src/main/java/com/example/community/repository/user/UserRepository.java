package com.example.community.repository.user;

import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.response.UserDeleteResponse;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    long addUser(UserDTO user);
    long getCountUser();
    Optional<UserDTO> findByProfileId(long profileId);
    Optional<UserDTO> findByEmail(String email);
    Optional<UserInfoDTO> findByNickname(String nickname);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoDTO updateUserInfo(UserInfoDTO userInfoDTO);
    void changePassword(long userNum, String nextPassword);
    UserDeleteResponse deleteUser(long profileId);
    Optional<UserInfoDTO> getUserInfo(long profileId);
    List<UserInfoDTO> getUserInfos(List<Long> profileIds);
}
