package com.example.community.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor
@Table(name="UserInfo")
public class UserInfo {
    @Id
    @Column(name = "userNum")
    private Long userNum;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "userNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SignInfo signInfo;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "profileImage")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
}
