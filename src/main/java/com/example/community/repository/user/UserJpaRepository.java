package com.example.community.repository.user;

import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.user.SignInfo;
import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserInfo;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.response.UserDeleteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepository implements UserRepository{
    private final SignInfoJpaRepository signInfoJPARepository;
    private final UserInfoJpaRepository userInfoJPARepository;

    @Override
    public long addUser(UserDTO user) {
        SignInfo signInfo = new SignInfo(user.getEmail(), user.getPassword());
        signInfoJPARepository.save(signInfo);

        UserInfo userInfo = new UserInfo(signInfo, user.getNickname(),
                user.getProfileImage());
        userInfoJPARepository.save(userInfo);

        return signInfo.getUserNum();
    }

    @Override
    public long getCountUser() {
        return signInfoJPARepository.count();
    }

    @Override
    public Optional<UserDTO> findByProfileId(long userNum) {
        SignInfo signInfo = signInfoJPARepository.findByUserNum(userNum)
                .orElse(null);
        if(signInfo == null){
            return Optional.empty();
        }

        UserInfo userInfo = userInfoJPARepository.findBySignInfo_UserNum(userNum).getFirst();

        return Optional.of(UserDTO.of(signInfo, userInfo));
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        SignInfo signInfo = signInfoJPARepository.findByEmail(email).orElse(null);
        if(signInfo == null){
            return Optional.empty();
        }
        UserInfo userInfo = userInfoJPARepository.findBySignInfo_UserNum(signInfo.getUserNum()).getFirst();

        return Optional.of(UserDTO.of(signInfo, userInfo));
    }

    @Override
    public Optional<UserInfoDTO> findByNickname(String nickname) {

        return userInfoJPARepository.findByNickname(nickname)
                .map(UserInfoDTO::from);
    }

    @Override
    public boolean isExistEmail(String email) {
        return signInfoJPARepository.existsByEmail(email);
    }

    @Override
    public boolean isExistNickname(String nickname) {
        return userInfoJPARepository.existsByNickname(nickname);
    }

    @Override
    public UserInfoDTO updateUserInfo(UserInfoDTO userInfoDTO)
    {
        UserInfo userInfo = userInfoJPARepository.findByProfileId(userInfoDTO.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 프로필"));
        userInfo.update(userInfoDTO.nickname(), userInfoDTO.profileImage());

        return UserInfoDTO.from(userInfo);
    }

    @Override
    public void changePassword(long userNum, String nextPassword) {
        SignInfo signInfo = signInfoJPARepository.findByUserNum(userNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        signInfo.changePassword(nextPassword);
        signInfoJPARepository.save(signInfo);
    }

    @Override
    public UserDeleteResponse deleteUser(long userNum) {
        SignInfo signInfo = signInfoJPARepository.findByUserNum(userNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        List<UserInfo> userInfo = userInfoJPARepository.findBySignInfo_UserNum(userNum);
        for(UserInfo ui : userInfo){
            ui.delete();
        }
        signInfo.delete();
        return UserDeleteResponse.from(signInfo);
    }

    @Override
    public Optional<UserInfoDTO> getUserInfo(long userNum) {
        return Optional.of(UserInfoDTO.from(userInfoJPARepository.findBySignInfo_UserNum(userNum).getFirst()));
    }

    @Override
    public List<UserInfoDTO> getUserInfos(List<Long> profileIds) {
        return userInfoJPARepository.findByProfileIdIn(profileIds).stream()
                .map(UserInfoDTO::from).toList();
    }
}
