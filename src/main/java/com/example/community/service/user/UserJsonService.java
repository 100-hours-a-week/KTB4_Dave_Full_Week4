package com.example.community.service.user;

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
import com.example.community.repository.user.UserRepository;
import com.example.community.resolver.SignUserInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserJsonService implements UserService{
    private final UserRepository userRepository;

    public UserJsonService(@Qualifier("userJsonRepository") UserRepository userRepository){
        this.userRepository = userRepository;
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
        user.setProfileImage(updateProfileImage(signUpRequest.imageFile()));
        user.setUserRole(UserRole.USER);
        return new SignUpResponse(userRepository.addUser(user));
    }

    public String updateProfileImage(MultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get("/app/uploads/profiles");
        Path targetPath = uploadPath.resolve(storedFileName);

        file.transferTo(targetPath);

        String imageUrl = "/images/profiles/" + storedFileName;

        return imageUrl;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 비어 있습니다.");
        }

        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase();
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
        long userNum = signUserInfo.userNum();

        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        userInfoDTO = userRepository.
               updateUserInfo(new UserInfoDTO(userNum, userNum, null, userInfoRequest.nickname(), updateProfileImage(userInfoRequest.imageFile()), userInfoDTO.userRole(), userInfoDTO.deletedAt()));
        return UserInfoResponse.from(userInfoDTO);
    }

    @Override
    public void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest) {
        long userNum = signUserInfo.userNum();
        UserDTO user = userRepository.findByProfileId(userNum)
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

        return userRepository.deleteUser(userNum);
    }
}
