package com.example.community.comment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
    private Comment comment;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "writeAt", nullable = false)
    private Instant writeAt = Instant.now();

    public static CommentEditRecord from(Comment comment){
        if(comment == null){
            throw new IllegalArgumentException("comment가 null");
        }
        return new CommentEditRecord(
                comment.getCommentNum(),
                comment.getComment(),
                comment.getVersion(),
                comment.getContent(),
                comment.getEditedAt() != null ? comment.getEditedAt() : comment.getWriteAt()
        );

    }
}
