package com.example.community.domain.user;

import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class UserDTO {
    private long userNum;
    private String email;
    private String password;
    private String nickname;
    private String profileImage;
    private boolean deleted = false;
    private UserRole userRole;

    public boolean passwordConfirm(String password){
        return this.password.equals(password);
    }

    public void delete(){
        deleted = true;
    }
}
