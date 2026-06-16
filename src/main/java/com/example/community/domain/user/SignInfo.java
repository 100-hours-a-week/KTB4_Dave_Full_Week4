package com.example.community.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name="SignInfo")
public class SignInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="userNum")
    private long userNum;

    @Column(name="email")
    private String email;

    @Column(name="password")
    private String password;

    @Column(name="deleted")
    private boolean deleted = false;

    @Column(name="lastLogin")
    private LocalDateTime lastLogin = LocalDateTime.now();

    public boolean passwordConfirm(String password){
        return this.password.equals(password);
    }
    public void delete(){
        deleted = true;
    }
}
