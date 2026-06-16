package com.example.community.domain.post;

import com.example.community.domain.user.SignInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table
public class TemporaryPost {
    @EmbeddedId
    private TemporaryPostId id;

    @MapsId("userNum")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", referencedColumnName = "userNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SignInfo signInfo;

    public TemporaryPost(SignInfo signInfo, UUID temporaryKey){
        this.signInfo = signInfo;
        this.id = new TemporaryPostId(signInfo.getUserNum(), temporaryKey);
    }
}
