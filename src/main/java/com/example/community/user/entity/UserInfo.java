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
        signInfo.delete();
    }


    public boolean isDeleted(){
        return deletedAt != null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserInfo userInfo)) {
            return false;
        }
        return profileId != null && profileId.equals(userInfo.getProfileId());
    }

    @Override
    public int hashCode() {
        return UserInfo.class.hashCode();
    }
}
