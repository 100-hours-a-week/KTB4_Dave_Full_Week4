package com.example.community.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor
@Table(name="SignInfo")
public class SignInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="profileId")
    private Long userNum;

    @Column(name="email", nullable = false, unique = true)
    private String email;

    @Column(name="password", nullable = false)
    private String password;

    @Column(name="deletedAt")
    private Instant deletedAt;

    @Column(name="lastLogin", nullable = false)
    private Instant lastLogin = Instant.now();

    public SignInfo(String email, String password){
        validateEmail(email);
        validatePassword(password);
        this.email = email;
        this.password = password;
        deletedAt = null;
    }

    public boolean passwordConfirm(String password){
        return this.password.equals(password);
    }

    private void validatePassword(String password){
        if(password.isBlank()){
            // 암호화된 비밀번호를 저장하게 되므로 여기서는 8자 이상 20자 이하 조건 검사 불가능
            throw new IllegalArgumentException("비밀번호는 8자 이상 20자 이하여야 합니다.");
        }
    }

    public void changePassword(String password){
        validatePassword(password);
        this.password = password;
    }

    public void loginSuccess(){
        lastLogin = Instant.now();
    }

    private void validateEmail(String email){
        Assert.hasText(email, "email은 필수입니다.");
        if(email.length() > 60){
            throw new IllegalArgumentException("이메일이 너무 깁니다.");
        }
    }
    public void delete(){
        deletedAt = Instant.now();
    }

    private boolean isDeleted(){
        return deletedAt != null;
    }

    public boolean equals(SignInfo signInfo){
        if(!this.userNum.equals(signInfo.getUserNum())) return false;
        if(!this.email.equals(signInfo.getEmail())) return false;
        if(!this.password.equals(signInfo.getPassword())) return false;
        if(!this.deletedAt.equals(signInfo.getDeletedAt())) return false;
        if(!this.lastLogin.equals(signInfo.getLastLogin())) return false;
        return true;
    }
}
