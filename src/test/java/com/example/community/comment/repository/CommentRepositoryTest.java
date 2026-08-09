package com.example.community.comment.repository;

import com.example.community.comment.dto.response.CommentResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {
    private static final Sort REGISTRATION_ORDER =
            Sort.by(Sort.Direction.ASC, "commentNum");

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
                new SignInfo("comment-repository@example.com", "password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "comment-repository", null)
        );
        post = savePost("post");
    }

    @Test
    @DisplayName("최상위 댓글이 없으면 빈 페이지를 반환한다")
    void findByPostPostNumReturnsEmptyPageWhenCommentDoesNotExist() {
        Page<Comment> result = commentRepository.findByPost_postNum(
                Long.MAX_VALUE,
                pageRequest()
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("해당 게시글의 최상위 댓글과 저장된 값을 반환한다")
    void findByPostPostNumReturnsRootCommentsForRequestedPost() {
        Comment root = saveRootComment(post, "root");
        saveChildComment(post, root, "child");
        Post otherPost = savePost("other-post");
        saveRootComment(otherPost, "other-root");

        Page<Comment> result = commentRepository.findByPost_postNum(
                post.getPostNum(),
                pageRequest()
        );

        assertThat(result.getContent())
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.getCommentNum())
                            .isEqualTo(root.getCommentNum());
                    assertThat(found.getPost().getPostNum())
                            .isEqualTo(post.getPostNum());
                    assertThat(found.getUserInfo().getProfileId())
                            .isEqualTo(author.getProfileId());
                    assertThat(found.getContent()).isEqualTo("root");
                    assertThat(found.getDepth()).isZero();
                });
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("삭제된 최상위 댓글도 트리 유지를 위해 반환한다")
    void findByPostPostNumIncludesDeletedRootComment() {
        Comment deleted = saveRootComment(post, "deleted-content");
        deleted.delete();
        commentRepository.flush();

        Page<Comment> result = commentRepository.findByPost_postNum(
                post.getPostNum(),
                pageRequest()
        );

        assertThat(result.getContent())
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.getCommentNum())
                            .isEqualTo(deleted.getCommentNum());
                    assertThat(found.isDeleted()).isTrue();
                    assertThat(found.getContent())
                            .isEqualTo("deleted-content");
                });
    }

    @Test
    @DisplayName("대댓글이 없으면 빈 페이지를 반환한다")
    void findByParentNumReturnsEmptyPageWhenChildDoesNotExist() {
        Page<Comment> result = commentRepository.findByParentNum(
                Long.MAX_VALUE,
                pageRequest()
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("해당 부모의 직계 대댓글과 저장된 값을 반환한다")
    void findByParentNumReturnsDirectChildrenForRequestedParent() {
        Comment parent = saveRootComment(post, "parent");
        Comment first = saveChildComment(post, parent, "first-child");
        Comment second = saveChildComment(post, parent, "second-child");
        saveChildComment(post, first, "grand-child");
        Comment otherParent = saveRootComment(post, "other-parent");
        saveChildComment(post, otherParent, "other-child");

        Page<Comment> result = commentRepository.findByParentNum(
                parent.getCommentNum(),
                pageRequest()
        );

        assertThat(result.getContent())
                .extracting(Comment::getCommentNum)
                .containsExactly(first.getCommentNum(), second.getCommentNum());
        assertThat(result.getContent().getFirst().getContent())
                .isEqualTo("first-child");
        assertThat(result.getContent().getFirst().getDepth()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getComment().getCommentNum())
                .isEqualTo(parent.getCommentNum());
    }

    @Test
    @DisplayName("댓글 번호에 해당하는 댓글이 없으면 빈 Optional을 반환한다")
    void findByCommentNumReturnsEmptyWhenCommentDoesNotExist() {
        assertThat(commentRepository.findByCommentNum(Long.MAX_VALUE))
                .isEmpty();
    }

    @Test
    @DisplayName("댓글 번호에 해당하는 저장된 댓글을 반환한다")
    void findByCommentNumReturnsStoredComment() {
        Comment comment = saveRootComment(post, "stored-content");

        assertThat(commentRepository.findByCommentNum(comment.getCommentNum()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getCommentNum())
                            .isEqualTo(comment.getCommentNum());
                    assertThat(found.getContent()).isEqualTo("stored-content");
                    assertThat(found.getPost().getPostNum())
                            .isEqualTo(post.getPostNum());
                    assertThat(found.getUserInfo().getProfileId())
                            .isEqualTo(author.getProfileId());
                });
    }

    @Test
    @DisplayName("삭제된 댓글은 수정·삭제 대상 조회에서 제외한다")
    void findByCommentNumDoesNotReturnDeletedComment() {
        Comment deleted = saveRootComment(post, "deleted");
        deleted.delete();
        commentRepository.flush();

        assertThat(commentRepository.findByCommentNum(
                deleted.getCommentNum()
        )).isEmpty();
    }

    @Test
    @DisplayName("부모 댓글 조회는 요청한 게시글에 속한 댓글만 반환한다")
    void findParentCommentRequiresMatchingPost() {
        Comment parent = saveRootComment(post, "parent");
        Post otherPost = savePost("other-post");

        assertThat(commentRepository
                .findByCommentNumAndPost_PostNumAndDeletedAtIsNull(
                        parent.getCommentNum(),
                        post.getPostNum()
                )).contains(parent);
        assertThat(commentRepository
                .findByCommentNumAndPost_PostNumAndDeletedAtIsNull(
                        parent.getCommentNum(),
                        otherPost.getPostNum()
                )).isEmpty();
    }

    @Test
    @DisplayName("캐시용 첫 페이지 projection은 최상위 댓글과 페이지 정보를 반환한다")
    void findFirstPageCacheValuesReturnsRootComments() {
        Comment first = saveRootComment(post, "first");
        Comment deleted = saveRootComment(post, "deleted-content");
        saveChildComment(post, first, "child");
        deleted.delete();
        commentRepository.flush();

        Page<CommentResponse> result = commentRepository
                .findFirstPageResponsesByPostNum(
                        post.getPostNum(),
                        pageRequest()
                );

        assertThat(result.getContent())
                .extracting(CommentResponse::commentNum)
                .containsExactly(first.getCommentNum(), deleted.getCommentNum());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(1).content())
                .isEqualTo("삭제된 댓글입니다.");
        assertThat(result.getContent().get(1).deleted()).isTrue();
    }

    @Test
    @DisplayName("캐시 부분 미스 IN projection은 삭제 댓글도 제외하지 않는다")
    void findCacheValuesByCommentNumInIncludesDeletedComments() {
        Comment visible = saveRootComment(post, "visible");
        Comment deleted = saveRootComment(post, "deleted-content");
        deleted.delete();
        commentRepository.flush();

        List<CommentResponse> result = commentRepository
                .findResponsesByCommentNumIn(List.of(
                        visible.getCommentNum(),
                        deleted.getCommentNum()
                ));

        assertThat(result)
                .extracting(CommentResponse::commentNum)
                .containsExactlyInAnyOrder(
                        visible.getCommentNum(),
                        deleted.getCommentNum()
                );
        assertThat(result)
                .filteredOn(value -> value.commentNum() == deleted.getCommentNum())
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.content()).isEqualTo("삭제된 댓글입니다.");
                    assertThat(value.deleted()).isTrue();
                });
    }

    @Test
    @DisplayName("캐시 projection 결과의 탈퇴 작성자는 Java DTO에서 마스킹한다")
    void cacheValueMasksDeletedAuthor() {
        Comment comment = saveRootComment(post, "content");
        author.delete();
        userInfoRepository.flush();

        CommentResponse result = commentRepository
                .findResponsesByCommentNumIn(List.of(
                        comment.getCommentNum()
                ))
                .getFirst();

        assertThat(result.nickname()).isEqualTo("탈퇴한 사용자");
        assertThat(result.profileImage()).isNull();
        assertThat(result.content()).isEqualTo("content");
    }

    private PageRequest pageRequest() {
        return PageRequest.of(0, 10, REGISTRATION_ORDER);
    }

    private Post savePost(String title) {
        return postRepository.saveAndFlush(
                new Post(author, title, "content", null)
        );
    }

    private Comment saveRootComment(Post targetPost, String content) {
        return commentRepository.saveAndFlush(
                new Comment(targetPost, author, content)
        );
    }

    private Comment saveChildComment(
            Post targetPost,
            Comment parent,
            String content
    ) {
        return commentRepository.saveAndFlush(
                new Comment(targetPost, parent, author, content)
        );
    }
}
