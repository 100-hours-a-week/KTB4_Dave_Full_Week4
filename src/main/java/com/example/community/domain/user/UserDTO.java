package com.example.community.domain.user;

import com.example.community.domain.user.request.SignUpRequest;
import com.example.community.domain.user.request.UserInfoRequest;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userNum;
    private Long profileId;
    private String email;
    private String password;
    private String nickname;
    private String profileImage;
    private Instant deletedAt;
    private UserRole userRole;

    public static UserDTO of(SignUpRequest signUpRequest){
        return new UserDTO(null, null, signUpRequest.email(), signUpRequest.password(), signUpRequest.nickname(),
                null, null, UserRole.USER);
    }

    public static UserDTO of(SignInfo signInfo, UserInfo userInfo){
        return new UserDTO(
                signInfo.getUserNum(),
                userInfo.getProfileId(),
                signInfo.getEmail(),
                signInfo.getPassword(),
                userInfo.getNickname(),
                userInfo.getProfileImage(),
                signInfo.getDeletedAt(),
                userInfo.getRole()
        );
    }

    public void update(UserInfoRequest userInfoRequest){
        this.nickname = userInfoRequest.nickname();
    }

    public boolean passwordConfirm(String password){
        return this.password.equals(password);
    }

    public void delete(){
        deletedAt = Instant.now();
    }
    public boolean isDeleted(){
        return deletedAt != null;
    }
}
