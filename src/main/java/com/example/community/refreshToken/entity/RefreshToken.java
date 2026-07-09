package com.example.community.refreshToken.entity;

import com.example.community.user.entity.SignInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RefreshToken")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refreshId")
    private Long refreshId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", nullable = false)
    private SignInfo signInfo;

    @Column(name = "token")
    private String token;
}
