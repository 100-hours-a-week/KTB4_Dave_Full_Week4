package com.example.community.comment.repository;

import com.example.community.comment.entity.Comment;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private UserInfo author;
    private Post post;

    @BeforeEach
    void setUp() {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo("comment-order@example.com", "password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "comment-order", null)
        );
        post = postRepository.saveAndFlush(
                new Post(author, "title", "content", null)
        );
    }

    @Test
    @DisplayName("최상위 댓글을 등록순으로 조회한다")
    void findPostCommentsInRegistrationOrder() {
        Comment first = saveRootComment("first");
        Comment second = saveRootComment("second");
        Comment third = saveRootComment("third");

        List<Long> result = commentRepository
                .findByPost_postNum(
                        post.getPostNum(),
                        PageRequest.of(0, 10)
                )
                .map(Comment::getCommentNum)
                .toList();

        assertThat(result).containsExactly(
                first.getCommentNum(),
                second.getCommentNum(),
                third.getCommentNum()
        );
    }

    @Test
    @DisplayName("대댓글을 등록순으로 조회한다")
    void findChildCommentsInRegistrationOrder() {
        Comment parent = saveRootComment("parent");
        Comment first = saveChildComment(parent, "first child");
        Comment second = saveChildComment(parent, "second child");

        List<Long> result = commentRepository
                .findByParentNum(
                        parent.getCommentNum(),
                        PageRequest.of(0, 10)
                )
                .map(Comment::getCommentNum)
                .toList();

        assertThat(result).containsExactly(
                first.getCommentNum(),
                second.getCommentNum()
        );
    }

    private Comment saveRootComment(String content) {
        return commentRepository.saveAndFlush(
                new Comment(post, author, content)
        );
    }

    private Comment saveChildComment(Comment parent, String content) {
        return commentRepository.saveAndFlush(
                new Comment(post, parent, author, content)
        );
    }
}
