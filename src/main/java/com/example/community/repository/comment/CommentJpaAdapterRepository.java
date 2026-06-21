package com.example.community.repository.comment;

import com.example.community.domain.comment.Comment;
import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.Post;
import com.example.community.domain.user.UserInfo;
import com.example.community.repository.post.PostJpaRepository;
import com.example.community.repository.user.UserInfoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentJpaAdapterRepository implements CommentRepository{
    private final CommentJpaRepository commentJpaRepository;
    private final UserInfoJpaRepository userInfoJpaRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public CommentDTO addComment(CommentDTO comment) {
        Post post = postJpaRepository.getReferenceById(comment.getPostNum());
        UserInfo userInfo = userInfoJpaRepository.getReferenceById(comment.getProfileId());
        Comment newComment;
        if(comment.getParentNum() != null){
            Comment parent = commentJpaRepository.getReferenceById(comment.getParentNum());
            newComment = new Comment(post, parent, userInfo, comment.getContent());
        }
        else {
            newComment = new Comment(post, userInfo, comment.getContent());
        }
        commentJpaRepository.save(newComment);
        return CommentDTO.from(newComment);
    }

    @Override
    public List<CommentDTO> getCommentsByPostNum(long postNum) {
        return commentJpaRepository.findByPost_postNum(postNum)
                .stream().map(CommentDTO::from).toList();
    }

    @Override
    public Optional<CommentDTO> getComment(long commentNum) {
        return Optional.of(CommentDTO.from(commentJpaRepository.findByCommentNum(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"))));
    }

    @Override
    public long getCommentCount() {
        return commentJpaRepository.count();
    }

    @Override
    public CommentDTO updateComment(long commentNum, String content) {
        Comment comment = commentJpaRepository.findByCommentNum(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        comment.update(content);
        return CommentDTO.from(comment);
    }

    @Override
    public void deleteComment(long commentNum) {
        Comment comment = commentJpaRepository.findByCommentNum(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        comment.delete();
    }
}
