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
    private Long commentEditId;

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

    public CommentEditRecord(Comment comment, Integer version,  String content, Instant writeAt){
        if(comment == null){
            throw new IllegalArgumentException("post가 null");
        }
        if(version == null || version < 1){
            throw new IllegalArgumentException("version은 양수여야 함");
        }
        if( content == null || content.isBlank()){
            throw new IllegalArgumentException("제목과 내용은 비어있으면 안됨");
        }
        if(writeAt == null){
            throw new IllegalArgumentException("writeTime이 null");
        }
        this.comment = comment;
        this.version = version;
        this.content = content;
        this.writeAt = writeAt;
    }
}
