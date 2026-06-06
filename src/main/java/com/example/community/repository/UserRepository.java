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
    Optional<UserInfoDTO> updateUserInfo(long userNum, String nickname, String profileImage);
    Optional<UserDeleteResponse> deleteUser(long userNum);
    List<UserInfoDTO> getUserInfos(List<Long> userNums);


}
