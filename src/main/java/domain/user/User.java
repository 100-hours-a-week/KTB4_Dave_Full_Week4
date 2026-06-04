package domain.user;

import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
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

    public String getNickname(){
        return isDeleted ? "알 수 없음" : nickname;
    }

    public void delete(){
        isDeleted = true;
    }
}
