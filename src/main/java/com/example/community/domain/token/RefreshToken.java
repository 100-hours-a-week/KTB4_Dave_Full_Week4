package com.example.community.domain.token;

import com.example.community.domain.user.SignInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refreshId")
    private Long refreshId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", nullable = false)
    private SignInfo signInfo;

    @Column(name = "jwtToken")
    private String token;
}
