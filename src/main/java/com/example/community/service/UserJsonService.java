package com.example.community.service;

import com.example.community.domain.token.Token;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.request.PasswordChangeRequest;
import com.example.community.domain.user.request.SignInRequest;
import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import com.example.community.domain.user.response.UserDeleteResponse;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.domain.user.response.UserResponse;
import com.example.community.repository.RefreshTokenRepository;
import com.example.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserJsonService implements UserService{
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserJsonService(@Qualifier("userJsonRepository") UserRepository userRepository,
                           @Qualifier("refreshTokenJsonRepository") RefreshTokenRepository refreshTokenRepository){
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @Override
    public long signUp(SignUpRequest signUpRequest) {
        if(isExistEmail(signUpRequest.email())){
            throw new RuntimeException("이메일 중복");
        }
        if(isExistNickname(signUpRequest.nickname())){
            throw new RuntimeException("닉네임 중복");
        }
        if(signUpRequest.password() != signUpRequest.passwordConfirm()){
            throw new RuntimeException("비밀번호 확인 불일치");
        }
        User user = new User();
        long userNum = userRepository.getAll().size()+1;
        user.setUserNum(userNum);
        user.setEmail(signUpRequest.email());
        user.setPassword(signUpRequest.password());
        user.setNickname(signUpRequest.nickname());
        user.setProfileImage(signUpRequest.profileImage());
        user.setUserRole(UserRole.USER);
        return userRepository.addUser(user);
    }

    @Override
    public UserResponse signIn(SignInRequest signInRequest) {
        User user = userRepository.findByEmail(signInRequest.email()).orElseThrow(
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
    public UserInfoResponse updateUserInfo(Token token, UserInfoRequest userInfoRequest) {
       UserInfoDTO userInfoDTO = userRepository.
               updateUserInfo(new UserInfoDTO(token.userNum(), userInfoRequest.nickname(), userInfoRequest.profileImage())).
               orElseThrow(() -> new RuntimeException("존재하지 않는 유저, 혹은 유효하지 않은 토큰일 듯")); // 여기도 수정 예정
       return UserInfoResponse.from(userInfoDTO);
    }

    @Override
    public void changePassword(Token token, PasswordChangeRequest passwordChangeRequest) {
        User user = userRepository.findByUserNum(token.userNum()).orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));
        if(!user.passwordConfirm(passwordChangeRequest.password())){
            throw new RuntimeException("현재 패스워드 틀림");
        }
        if(!passwordChangeRequest.nextPassword().equals(passwordChangeRequest.passwordConfirm())){
            throw new RuntimeException("패스워드 확인 틀림");
        }
        userRepository.changePassword(token.userNum(), passwordChangeRequest.nextPassword());

    }

    @Override
    public UserDeleteResponse deleteUser(Token token) {
        return userRepository.deleteUser(token.userNum()).orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));
    }
}
