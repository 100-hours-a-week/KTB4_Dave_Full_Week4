package repository;

import domain.user.User;
import domain.user.UserInfoDTO;
import domain.user.response.UserDeleteResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserJsonRepository implements UserRepository{
    private static final Resource resource = new ClassPathResource("data/userdata.json");

    @Override
    public long addUser(User User) {
        return 0;
    }

    @Override
    public List<User> getAll() {
        return List.of();
    }

    @Override
    public Optional<User> findByUserNum(long userNum) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public boolean isExistEmail(String Email) {
        return false;
    }

    @Override
    public boolean isExistNickname(String nickname) {
        return false;
    }

    @Override
    public UserInfoDTO updateUserInfo(long userNum, String nickname, String profileImage) {
        return null;
    }

    @Override
    public UserDeleteResponse deleteUser(long userNum) {
        return null;
    }

    @Override
    public UserInfoDTO getUserInfos(List<Long> userNums) {
        return null;
    }
}
