package com.example.community.user.repository;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepository implements UserRepository{
    private final SignInfoRepository signInfoRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    public UserDTO addUser(UserDTO user) {
        SignInfo signInfo = new SignInfo(user.getEmail(), user.getPassword());
        signInfoRepository.save(signInfo);

        UserInfo userInfo = new UserInfo(signInfo, user.getNickname(),
                user.getProfileImage());
        userInfoRepository.save(userInfo);

        return UserDTO.of(signInfo,userInfo);
    }

    @Override
    public long getCountUser() {
        return signInfoRepository.count();
    }

    @Override
    public Optional<UserDTO> findByUserNum(long userNum) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElse(null);
        if(signInfo == null){
            return Optional.empty();
        }
        UserInfo userInfo = userInfoRepository.findBySignInfo_UserNum(userNum).getFirst();


        return Optional.of(UserDTO.of(signInfo, userInfo));
    }

    @Override
    public Optional<UserInfoDTO> findByProfileId(long userNum) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElse(null);
        if(signInfo == null){
            return Optional.empty();
        }

        UserInfo userInfo = userInfoRepository.findBySignInfo_UserNum(userNum).getFirst();

        return Optional.of(UserInfoDTO.from(userInfo));
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        SignInfo signInfo = signInfoRepository.findByEmail(email).orElse(null);
        if(signInfo == null){
            return Optional.empty();
        }
        UserInfo userInfo = userInfoRepository.findBySignInfo_UserNum(signInfo.getUserNum()).getFirst();

        return Optional.of(UserDTO.of(signInfo, userInfo));
    }

    @Override
    public Optional<UserInfoDTO> findByNickname(String nickname) {

        return userInfoRepository.findByNickname(nickname)
                .map(UserInfoDTO::from);
    }

    @Override
    public boolean isExistEmail(String email) {
        return signInfoRepository.existsByEmail(email);
    }

    @Override
    public boolean isExistNickname(String nickname) {
        return userInfoRepository.existsByNickname(nickname);
    }

    @Override
    public UserInfoDTO updateUserInfo(long profileId, String nickname, String profileImage)
    {
        UserInfo userInfo = userInfoRepository.findByProfileId(profileId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 프로필"));
        userInfo.update(nickname, profileImage);

        return UserInfoDTO.from(userInfo);
    }

    @Override
    public void changePassword(long userNum, String nextPassword) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        signInfo.changePassword(nextPassword);
        signInfoRepository.save(signInfo);
    }

    @Override
    public Instant deleteUser(long userNum) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        List<UserInfo> userInfo = userInfoRepository.findBySignInfo_UserNum(userNum);
        for(UserInfo ui : userInfo){
            ui.delete();
        }
        signInfo.delete();
        return signInfo.getDeletedAt();
    }

    @Override
    public Optional<UserInfoDTO> getUserInfo(long userNum) {
        return Optional.of(UserInfoDTO.from(userInfoRepository.findBySignInfo_UserNum(userNum).getFirst()));
    }

    @Override
    public List<UserInfoDTO> getUserInfos(List<Long> profileIds) {
        return userInfoRepository.findByProfileIdIn(profileIds).stream()
                .map(UserInfoDTO::from).toList();
    }
}
