package com.example.community.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(name="UserInfo")
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profileId")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public void update(String nickname, String profileImage){
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public void delete(){
        deletedAt = Instant.now();
    }


    private boolean isDeleted(){
        return deletedAt != null;
    }


}
