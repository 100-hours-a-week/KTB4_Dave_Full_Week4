package com.example.community.user.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.entity.UserRole;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.repository.UserRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.util.ImageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserJsonService implements UserService{
    private final UserRepository userRepository;
    private final ImageConverter imageConverter;

    public UserJsonService(@Qualifier("userJsonRepository") UserRepository userRepository,
                           ImageConverter imageConverter){
        this.userRepository = userRepository;
        this.imageConverter = imageConverter;
    }


    @Override
    public SignUpResponse signUp(SignUpRequest signUpRequest) throws IOException {
        if(isExistEmail(signUpRequest.email())){
            throw new RuntimeException("이메일 중복");
        }
        if(isExistNickname(signUpRequest.nickname())){
            throw new RuntimeException("닉네임 중복");
        }
        System.out.println(signUpRequest.password() + " " + signUpRequest.passwordConfirm());
        if(!signUpRequest.password().equals(signUpRequest.passwordConfirm())){
            throw new RuntimeException("비밀번호 확인 불일치");
        }
        UserDTO user = new UserDTO();
        long userNum = userRepository.getCountUser()+1;
        user.setUserNum(userNum);
        user.setProfileId(userNum);
        user.setEmail(signUpRequest.email());
        user.setPassword(signUpRequest.password());
        user.setNickname(signUpRequest.nickname());
        user.setProfileImage(imageConverter.updateProfileImage(signUpRequest.imageFile()));
        user.setUserRole(UserRole.USER);
        return new SignUpResponse(userRepository.addUser(user).getUserNum());
    }

    @Override
    public UserInfoDTO signIn(SignInRequest signInRequest) {
        UserDTO user = userRepository.findByEmail(signInRequest.email()).orElseThrow(
                () -> new RuntimeException("이메일 혹은 비밀번호를 확인해 주세요.") // 사용자 지정 예외로 대체 예정
        );
        if(!user.passwordConfirm(signInRequest.password())){
            throw new RuntimeException("이메일 혹은 비밀번호를 확인해 주세요.");
        }

        return UserInfoDTO.from(user);
    }


    @Override
    public boolean isExistEmail(String email) {
        return userRepository.isExistEmail(email);
    }

    @Override
    public boolean isExistNickname(String nickname) {
        return userRepository.isExistNickname(nickname);
    }

    @Override
    public UserInfoResponse updateUserInfo(SignUserInfo signUserInfo, UserInfoRequest userInfoRequest) throws IOException {
        long profileId = signUserInfo.profileId();

        userRepository.findByProfileId(profileId).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        UserInfoDTO userInfoDTO = userRepository.
               updateUserInfo(profileId, userInfoRequest.nickname(), imageConverter.updateProfileImage(userInfoRequest.imageFile()));
        return UserInfoResponse.from(userInfoDTO);
    }

    @Override
    public void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest) {
        long userNum = signUserInfo.userNum();
        UserDTO user = userRepository.findByUserNum(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        if(!user.passwordConfirm(passwordChangeRequest.password())){
            throw new RuntimeException("현재 패스워드 틀림");
        }
        if(!passwordChangeRequest.nextPassword().equals(passwordChangeRequest.passwordConfirm())){
            throw new RuntimeException("패스워드 확인 틀림");
        }
        userRepository.changePassword(userNum, passwordChangeRequest.nextPassword());

    }

    @Override
    public UserDeleteResponse deleteUser(SignUserInfo signUserInfo) {
        long userNum = signUserInfo.userNum();

        return new UserDeleteResponse(userNum, userRepository.deleteUser(userNum) != null);
    }
}
