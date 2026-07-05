package com.example.community.user.repository;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.util.DataManager;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class UserJsonRepository implements UserRepository{
    private final DataManager<UserDTO> dataManager;

    public UserJsonRepository(@Qualifier("userDataManager") DataManager<UserDTO> dataManager){
        this.dataManager = dataManager;
    }

    @Override
    public UserDTO addUser(UserDTO user) {
        List<UserDTO> users = getAll();
        users.add(user);

        dataManager.writeData(users);
        return user;
    }

    @Override
    public long getCountUser() {
        return getAll().size();
    }

    @Override
    public Optional<UserDTO> findByUserNum(long userNum) {
        for(UserDTO u : getAll()){
            if(u.getUserNum() == userNum){
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }


    private List<UserDTO> getAll() {
        return dataManager.readData();
    }

    @Override
    public Optional<UserInfoDTO> findByProfileId(long profileId) {
        for(UserDTO u : getAll()){
            if(u.getProfileId() == profileId){
                return Optional.of(UserInfoDTO.from(u));
            }
        }


        return Optional.empty();
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        for(UserDTO u : getAll()){
            if(u.getEmail().equals(email)){
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<UserInfoDTO> findByNickname(String nickname){
        for(UserDTO u : getAll()){
            if(u.getNickname().equals(nickname)){
                UserInfoDTO userInfoDTO = UserInfoDTO.from(u);
                return Optional.of(userInfoDTO);
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
    public UserInfoDTO updateUserInfo(long profileId, String nickname, String profileImage) {
        List<UserDTO> users = getAll();
        for(UserDTO u : users){
            if(u.getProfileId() == profileId){
                u.setNickname(nickname);
                u.setProfileImage(profileImage);
                dataManager.writeData(users);
                return UserInfoDTO.from(u);
            }
        }
        throw new NotFoundException("존재하지 않는 유저"); // 커스텀 예외로 변경
    }

    @Override
    public void changePassword(long userNum, String nextPassword) {
        List<UserDTO> users = getAll();
        for(UserDTO u : users){
            if(u.getUserNum() == userNum){
                u.setPassword(nextPassword);
                dataManager.writeData(users);
                return;
            }
        }
        throw new NotFoundException("존재하지 않는 유저"); // 커스텀 예외로 변경
    }

    @Override
    public Instant deleteUser(long userNum) {
        List<UserDTO> users = getAll();
        for(UserDTO u : users){
            if(u.getUserNum() == userNum){
                u.delete();
                dataManager.writeData(users);
                return u.getDeletedAt();
            }
        }
        throw new NotFoundException("존재하지 않는 유저"); // 커스텀 예외로 변경
    }

    @Override
    public Optional<UserInfoDTO> getUserInfo(long userNum) {
        for(UserDTO u : getAll()){
            if(u.getUserNum() == userNum){
                return Optional.of(UserInfoDTO.from(u));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<UserInfoDTO> getUserInfos(List<Long> profileIds) {
        return getAll().stream().
                filter(u -> profileIds.contains(u.getUserNum())).
                map(UserInfoDTO::from).toList();
    }

}
