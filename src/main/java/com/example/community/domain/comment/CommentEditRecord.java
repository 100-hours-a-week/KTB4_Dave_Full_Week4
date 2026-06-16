package com.example.community.domain.comment;

import com.example.community.domain.post.Post;
import com.example.community.domain.post.PostEditRecordId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "CommentEditRecord")
public class CommentEditRecord {
    @EmbeddedId
    private CommentEditRecordId id;

    @MapsId("commentNum")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commentNum", referencedColumnName = "commentNum")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "writeTime", nullable = false)
    private LocalDateTime writeTime = LocalDateTime.now();

    public CommentEditRecord(Comment comment, Integer version,  String content, LocalDateTime writeTime){
        if(comment == null){
            throw new IllegalArgumentException("post가 null");
        }
        if(version == null || version < 1){
            throw new IllegalArgumentException("version은 양수여야 함");
        }
        if( content == null || content.isBlank()){
            throw new IllegalArgumentException("제목과 내용은 비어있으면 안됨");
        }
        if(writeTime == null){
            throw new IllegalArgumentException("writeTime이 null");
        }
        this.comment = comment;
        this.content = content;
        this.writeTime = writeTime;
        this.id = new CommentEditRecordId(comment.getCommentNum(), version);
    }
}
