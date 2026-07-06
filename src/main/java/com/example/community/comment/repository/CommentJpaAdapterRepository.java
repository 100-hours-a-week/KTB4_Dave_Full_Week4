package com.example.community.comment.repository;

import com.example.community.comment.entity.Comment;
import com.example.community.comment.dto.CommentDTO;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserInfo;
import com.example.community.post.repository.PostJpaRepository;
import com.example.community.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentJpaAdapterRepository implements CommentRepository{
    private final CommentJpaRepository commentJpaRepository;
    private final UserInfoRepository userInfoRepository;
    private final PostJpaRepository postJpaRepository;

    @Override
    public CommentDTO addComment(CommentDTO comment) {
        Post post = postJpaRepository.getReferenceById(comment.getPostNum());
        UserInfo userInfo = userInfoRepository.getReferenceById(comment.getProfileId());
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
        Optional<Comment> comment = commentJpaRepository.findByCommentNum(commentNum);

        return comment.map(CommentDTO::from);
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
        commentJpaRepository.save(comment);
        return CommentDTO.from(comment);
    }

    @Override
    public void deleteComment(long commentNum) {
        Comment comment = commentJpaRepository.findByCommentNum(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        comment.delete();
        commentJpaRepository.save(comment);
    }
}
