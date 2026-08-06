package com.example.community.comment.repository;

import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentEditRepositoryTest {
    @Autowired
    private CommentEditRepository commentEditRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private UserInfo author;
    private Comment comment;

    @BeforeEach
    void setUp() {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo("comment-edit@example.com", "password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "comment-edit", null)
        );
        Post post = savePost("title");
        comment = saveComment(post, "current-content");
    }

    @Test
    @DisplayName("수정 이력이 없으면 빈 페이지를 반환한다")
    void findByCommentNumReturnsEmptyPageWhenEditDoesNotExist() {
        Page<CommentEditRecord> result = commentEditRepository
                .findByComment_CommentNumOrderByEditIdDesc(
                        Long.MAX_VALUE,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("해당 댓글의 수정 이력과 저장된 값을 최신순으로 반환한다")
    void findByCommentNumReturnsStoredEditsInDescendingOrder() {
        CommentEditRecord first = saveEdit(comment, 0, "first");
        CommentEditRecord second = saveEdit(comment, 1, "second");
        Comment otherComment = saveComment(comment.getPost(), "other");
        saveEdit(otherComment, 0, "other-edit");

        Page<CommentEditRecord> result = commentEditRepository
                .findByComment_CommentNumOrderByEditIdDesc(
                        comment.getCommentNum(),
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .extracting(CommentEditRecord::getEditId)
                .containsExactly(second.getEditId(), first.getEditId());
        assertThat(result.getContent().getFirst()).satisfies(found -> {
            assertThat(found.getComment().getCommentNum())
                    .isEqualTo(comment.getCommentNum());
            assertThat(found.getVersion()).isEqualTo(1);
            assertThat(found.getContent()).isEqualTo("second");
            assertThat(found.getWriteAt())
                    .isEqualTo(Instant.parse("2026-08-05T00:00:01Z"));
        });
    }

    private Post savePost(String title) {
        return postRepository.saveAndFlush(
                new Post(author, title, "content", null)
        );
    }

    private Comment saveComment(Post post, String content) {
        return commentRepository.saveAndFlush(
                new Comment(post, author, content)
        );
    }

    private CommentEditRecord saveEdit(
            Comment targetComment,
            int version,
            String content
    ) {
        return commentEditRepository.saveAndFlush(
                new CommentEditRecord(
                        null,
                        targetComment,
                        version,
                        content,
                        Instant.parse("2026-08-05T00:00:0" + version + "Z")
                )
        );
    }
}
