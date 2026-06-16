package com.example.community.repository;

import com.example.community.domain.exception.NotFoundException;
import com.example.community.util.DataManager;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.response.UserDeleteResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserJsonRepository implements UserRepository{
    private final DataManager<User> dataManager;

    public UserJsonRepository(@Qualifier("userDataManager") DataManager<User> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public long addUser(User user) {
        List<User> users = getAll();
        users.add(user);

        dataManager.writeData(users);
        return user.getUserNum();
    }

    @Override
    public List<User> getAll() {
        return dataManager.readData();
    }

    @Override
    public Optional<User> findByUserNum(long userNum) {
        for(User u : getAll()){
            if(u.getUserNum() == userNum){
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        for(User u : getAll()){
            if(u.getEmail().equals(email)){
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByNickname(String nickname){
        for(User u : getAll()){
            if(u.getNickname().equals(nickname)){
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isExistEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean isExistNickname(String nickname) {
        return findByNickname(nickname).isPresent();
    }

    @Override
    public UserInfoDTO updateUserInfo(UserInfoDTO userInfoDTO) {
        List<User> users = getAll();
        for(User u : users){
            if(u.getUserNum() == userInfoDTO.userNum()){
                u.setNickname(userInfoDTO.nickname());
                u.setProfileImage(userInfoDTO.profileImage());
                dataManager.writeData(users);
                return userInfoDTO;
            }
        }
        throw new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND); // 커스텀 예외로 변경
    }

    @Override
    public void changePassword(long userNum, String nextPassword) {
        List<User> users = getAll();
        for(User u : users){
            if(u.getUserNum() == userNum){
                u.setPassword(nextPassword);
                dataManager.writeData(users);
                return;
            }
        }
        throw new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND); // 커스텀 예외로 변경
    }

    @Override
    public UserDeleteResponse deleteUser(long userNum) {
        List<User> users = getAll();
        for(User u : users){
            if(u.getUserNum() == userNum){
                u.delete();
                dataManager.writeData(users);
                return UserDeleteResponse.from(u);
            }
        }
        throw new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND); // 커스텀 예외로 변경
    }

    @Override
    public Optional<UserInfoDTO> getUserInfo(long userNum) {
        for(User u : getAll()){
            if(u.getUserNum() == userNum){
                return Optional.of(UserInfoDTO.from(u));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<UserInfoDTO> getUserInfos(List<Long> userNums) {
        return getAll().stream().
                filter(u -> userNums.contains(u.getUserNum())).
                map(UserInfoDTO::from).toList();
    }

}
