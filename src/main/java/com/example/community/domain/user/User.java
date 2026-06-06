package com.example.community.domain.user;

import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class User {
    private long userNum;
    private String email;
    private String password;
    private String nickname;
    private String profileImage;
    private boolean isDeleted = false;

    public boolean passwordConfirm(String password){
        return this.password.equals(password);
    }

    public void delete(){
        isDeleted = true;
    }
}
