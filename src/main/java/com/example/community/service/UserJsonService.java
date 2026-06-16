package com.example.community.service;

import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.SignUpResponse;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.domain.user.response.UserResponse;
import com.example.community.repository.UserRepository;
import com.example.community.util.JWTUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserJsonService implements UserService{
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    public UserJsonService(@Qualifier("userJsonRepository") UserRepository userRepository,
                           JWTUtil jwtUtil){
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }


    @Override
    public SignUpResponse signUp(SignUpRequest signUpRequest) {
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
        long userNum = userRepository.getAll().size()+1;
        user.setUserNum(userNum);
        user.setEmail(signUpRequest.email());
        user.setPassword(signUpRequest.password());
        user.setNickname(signUpRequest.nickname());
        user.setProfileImage(signUpRequest.profileImage());
        user.setUserRole(UserRole.USER);
        return new SignUpResponse(userRepository.addUser(user));
    }

    @Override
    public UserResponse signIn(SignInRequest signInRequest) {
        UserDTO user = userRepository.findByEmail(signInRequest.email()).orElseThrow(
                () -> new RuntimeException("이메일 혹은 비밀번호를 확인해 주세요.") // 사용자 지정 예외로 대체 예정
        );
        if(!user.passwordConfirm(signInRequest.password())){
            throw new RuntimeException("이메일 혹은 비밀번호를 확인해 주세요.");
        }

        return UserResponse.from(user);
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
    public UserInfoResponse updateUserInfo(String token, UserInfoRequest userInfoRequest) {
        long userNum = jwtUtil.getUidFromToken(token);

        UserInfoDTO userInfoDTO = userRepository.
               updateUserInfo(new UserInfoDTO(userNum, userInfoRequest.nickname(), userInfoRequest.profileImage(), true));
        return UserInfoResponse.from(userInfoDTO);
    }

    @Override
    public void changePassword(String token, PasswordChangeRequest passwordChangeRequest) {
        long userNum = jwtUtil.getUidFromToken(token);
        UserDTO user = userRepository.findByUserNum(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND));
        if(!user.passwordConfirm(passwordChangeRequest.password())){
            throw new RuntimeException("현재 패스워드 틀림");
        }
        if(!passwordChangeRequest.nextPassword().equals(passwordChangeRequest.passwordConfirm())){
            throw new RuntimeException("패스워드 확인 틀림");
        }
        userRepository.changePassword(userNum, passwordChangeRequest.nextPassword());

    }

    @Override
    public UserDeleteResponse deleteUser(String token) {
        long userNum = jwtUtil.getUidFromToken(token);

        return userRepository.deleteUser(userNum);
    }
}
