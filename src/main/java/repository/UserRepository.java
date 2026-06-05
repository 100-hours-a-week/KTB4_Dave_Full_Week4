package repository;

import domain.user.UserInfoDTO;
import domain.user.User;
import domain.user.UserLikePost;
import domain.user.response.UserDeleteResponse;

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
