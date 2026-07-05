package com.example.community.user.dto;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
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

    public boolean equals(UserDTO userDTO){
        if(!this.userNum.equals(userDTO.getUserNum())) return false;
        if(!this.profileId.equals(userDTO.getProfileId())) return false;
        if(!this.email.equals(userDTO.getEmail())) return false;
        if(!this.password.equals(userDTO.getPassword())) return false;
        if(!this.nickname.equals(userDTO.getNickname())) return false;
        if(this.profileImage != null && userDTO.getProfileImage() != null) {
            if (!this.profileImage.equals(userDTO.getProfileImage())) return false;
        }
        else if(!(this.profileImage == null && userDTO.getProfileImage() == null)) return false;
        if(this.deletedAt != null && userDTO.getDeletedAt() != null) {
            if(!this.deletedAt.equals(userDTO.getDeletedAt())) return false;
        }
        else if(!(this.deletedAt == null && userDTO.getDeletedAt() == null)) return false;
        if(!this.userRole.equals(userDTO.getUserRole())) return false;
        return true;
    }
}
