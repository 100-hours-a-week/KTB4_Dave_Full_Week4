package com.example.community.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="UserInfo")
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profileId")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", nullable = false)
    private SignInfo signInfo;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;


    @Column(name = "profileImage")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    public UserInfo(SignInfo signInfo, String nickname, String profileImage){
        this.signInfo = signInfo;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public void setProfileId(long profileId){
        this.profileId = profileId;
    }

    public String getNickname(){
        return isDeleted() ? "알 수 없음" : nickname;
    }

    public String getProfileImage(){
        return isDeleted() ? null : profileImage;
    }

    public void update(String nickname, String profileImage){
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public void delete(){
        deletedAt = Instant.now();
    }


    public boolean isDeleted(){
        return deletedAt != null;
    }

    public boolean equals(UserInfo userInfo){
        if(!this.profileId.equals(userInfo.getProfileId())) return false;
        if(!this.signInfo.equals(userInfo.getSignInfo())) return false;
        if(!this.nickname.equals(userInfo.getNickname())) return false;
        if(!this.profileImage.equals(userInfo.getProfileImage())) return false;
        if(!this.role.equals(userInfo.getRole())) return false;
        if(!this.deletedAt.equals(userInfo.getDeletedAt())) return false;
        return true;
    }
}
