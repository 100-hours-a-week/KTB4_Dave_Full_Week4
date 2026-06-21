package com.example.community.service.user;

import com.example.community.domain.exception.BadRequestException;
import com.example.community.domain.exception.DuplicateException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.exception.UnAuthorizedException;
import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.SignUpResponse;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.user.UserRepository;
import com.example.community.resolver.SignUserInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class UserJpaService implements UserService{
    private final UserRepository userRepository;

    public UserJpaService(@Qualifier("userJpaRepository") UserRepository userRepository){
        this.userRepository = userRepository;
    }


    @Override
    public SignUpResponse signUp( SignUpRequest signUpRequest) {
        if(userRepository.isExistEmail(signUpRequest.email())){
            throw new DuplicateException("중복 이메일 존재");
        }
        if(userRepository.isExistNickname(signUpRequest.nickname())){
            throw new DuplicateException("중복 닉네임 존재");
        }
        if(!signUpRequest.password().equals(signUpRequest.passwordConfirm())){
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        return new SignUpResponse(userRepository.addUser(UserDTO.of(signUpRequest)));
    }

    @Override
    public UserInfoDTO signIn(SignInRequest signInRequest) {
        UserDTO userDTO = userRepository.findByEmail(signInRequest.email())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 이메일"));

        //현재는 평문이 저장되겠으나 암호화된 값을 비교해야 함.
        if(!userDTO.passwordConfirm(signInRequest.password())){
            throw new UnAuthorizedException("로그인 실패");
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
    public UserInfoResponse updateUserInfo(SignUserInfo signUserInfo, UserInfoRequest userInfoRequest) {
        UserDTO userDTO = userRepository.findByProfileId(signUserInfo.userNum())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        if(isExistNickname(userInfoRequest.nickname())){
            throw new DuplicateException("중복 이메일 존재");
        }
        userDTO.update(userInfoRequest);
        return UserInfoResponse.from(userRepository.updateUserInfo(UserInfoDTO.from(userDTO)));
    }

    @Override
    public void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest) {
        UserDTO userDTO = userRepository.findByProfileId(signUserInfo.userNum())
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
        return userRepository.deleteUser(signUserInfo.userNum());
    }
}
