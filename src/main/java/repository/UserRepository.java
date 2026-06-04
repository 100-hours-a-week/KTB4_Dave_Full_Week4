package repository;

import domain.user.UserInfoDTO;
import domain.user.User;
import domain.user.response.UserDeleteResponse;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    long addUser(User User);
    List<User> getAll();
    Optional<User> findByUserNum(long userNum);
    Optional<User> findByEmail(String email);
    boolean isExistEmail(String Email);
    boolean isExistNickname(String nickname);
    UserInfoDTO updateUserInfo(long userNum, String nickname, String profileImage);
    UserDeleteResponse deleteUser(long userNum);
    UserInfoDTO getUserInfos(List<Long> userNums);
}
