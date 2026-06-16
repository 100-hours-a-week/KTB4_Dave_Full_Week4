package com.example.community.domain.comment;

import com.example.community.domain.post.Post;
import com.example.community.domain.user.SignInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "Comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentNum")
    private Long commentNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postNum", referencedColumnName = "postNum", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentNum", referencedColumnName = "commentNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Column(name = "depth")
    private Integer depth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userNum", referencedColumnName = "userNum", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SignInfo signInfo;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "edited", nullable = false)
    private Boolean edited = false;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "saveTime", nullable = false)
    private LocalDateTime saveTime = LocalDateTime.now();

    @Column(name = "writeTime", nullable = false)
    private LocalDateTime writeTime = LocalDateTime.now();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    public void update(String content){
        this.content = content;
        edited = true;
        saveTime = LocalDateTime.now();
    }

    public void delete(){
        this.deleted = true;
    }
}
