package com.example.community.domain.post;

import com.example.community.domain.user.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "TemporaryPost",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_TemporaryPost_userNum_temporaryKey",
                        columnNames = {"profileId", "temporaryKey"}
                )
        }
)
public class TemporaryPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="temporaryId")
    private Long temporaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profileId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserInfo userInfo;

    @Column(name = "temporaryKey", nullable = false, unique = true)
    private UUID temporaryKey;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "image")
    private String image;

    @Column(name = "writeAt", nullable = false)
    private Instant writeAt;

    public TemporaryPost(UserInfo userInfo, UUID temporaryKey){
        this.userInfo = userInfo;
        this.temporaryKey = temporaryKey;
    }
}
