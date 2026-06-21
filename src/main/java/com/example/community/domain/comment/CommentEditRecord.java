package com.example.community.domain.comment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "CommentEditRecord",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_CommentEditRecord_commentNum_version",
                        columnNames = {"commentNum", "version"}
                )
        }
)
public class CommentEditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commentEditId")
    private Long editId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commentNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "writeAt", nullable = false)
    private Instant writeAt = Instant.now();

    public CommentEditRecord(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("comment가 null");
        }
        this.comment = comment;
        this.version = comment.getVersion();
        this.content = comment.getContent();
        this.writeAt = comment.getEditedAt();
    }
}
