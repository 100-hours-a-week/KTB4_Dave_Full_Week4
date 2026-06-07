package com.example.community.repository;

import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.User;
import com.example.community.domain.user.response.UserDeleteResponse;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    long addUser(User user);
    List<User> getAll();
    Optional<User> findByUserNum(long userNum);
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean isExistEmail(String email);
    boolean isExistNickname(String nickname);
    UserInfoDTO updateUserInfo(UserInfoDTO userInfoDTO);
    void changePassword(long userNum, String nextPassword);
    UserDeleteResponse deleteUser(long userNum);
    Optional<UserInfoDTO> getUserInfo(long userNum);
    List<UserInfoDTO> getUserInfos(List<Long> userNums);
}
