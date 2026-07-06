package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;
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
public class UserJpaService implements UserService{
    private final UserRepository userRepository;
    private final ImageConverter imageConverter;

    public UserJpaService(@Qualifier("userJpaRepository") UserRepository userRepository,
                          ImageConverter imageConverter){
        this.userRepository = userRepository;
        this.imageConverter = imageConverter;
    }


    @Override
    public SignUpResponse signUp( SignUpRequest signUpRequest) throws IOException {
        if(userRepository.isExistEmail(signUpRequest.email())){
            throw new DuplicateException("중복 이메일 존재");
        }
        if(userRepository.isExistNickname(signUpRequest.nickname())){
            throw new DuplicateException("중복 닉네임 존재");
        }
        if(!signUpRequest.password().equals(signUpRequest.passwordConfirm())){
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        UserDTO userDTO = UserDTO.of(signUpRequest);
        if(signUpRequest.imageFile() != null) {
            userDTO.setProfileImage(imageConverter.updateProfileImage(signUpRequest.imageFile()));
        }

        return new SignUpResponse(userRepository.addUser(userDTO).getUserNum());
    }

    @Override
    public UserInfoDTO signIn(SignInRequest signInRequest) {
        UserDTO userDTO = userRepository.findByEmail(signInRequest.email())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 이메일"));

        //현재는 평문이 저장되겠으나 암호화된 값을 비교해야 함.
        if(!userDTO.passwordConfirm(signInRequest.password())){
            throw new UnAuthorizedException("로그인 실패");
        }
        if(userDTO.isDeleted()){
            throw new UnAuthorizedException("탈퇴한 유저");
        }

        return UserInfoDTO.from(userDTO);
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
        UserInfoDTO userDTO = userRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        if(isExistNickname(userInfoRequest.nickname())){
            throw new DuplicateException("중복 닉네임 존재");
        }
        userDTO.setNickname(userInfoRequest.nickname());
        String profileImage = null;
        if(userInfoRequest.imageFile() != null) {
            profileImage = imageConverter.updateProfileImage(userInfoRequest.imageFile());
            userDTO.setProfileImage(profileImage);
        }
        return UserInfoResponse.from(userRepository.updateUserInfo(signUserInfo.profileId(), userInfoRequest.nickname(), profileImage));
    }

    @Override
    public void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest) {
        UserDTO userDTO = userRepository.findByUserNum(signUserInfo.userNum())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        if(!userDTO.passwordConfirm(passwordChangeRequest.password())){
            throw new BadRequestException("비밀번호가 틀렸습니다.");
        }
        if(!passwordChangeRequest.nextPassword().equals(passwordChangeRequest.passwordConfirm())){
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        userRepository.changePassword(signUserInfo.userNum(), passwordChangeRequest.nextPassword());
    }

    @Override
    public UserDeleteResponse deleteUser(SignUserInfo signUserInfo) {
        return new UserDeleteResponse(signUserInfo.userNum(), userRepository.deleteUser(signUserInfo.userNum()) != null);
    }
}
