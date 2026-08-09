package com.example.community.comment.service;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.comment.event.CommentChangedEvent;
import com.example.community.comment.repository.CommentEditRepository;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCommandService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserInfoRepository userInfoRepository;
    private final CommentEditRepository commentEditRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentAddResponse addCommentToPost(
            SignUserInfo signUserInfo,
            long postNum,
            CommentToPostRequest request
    ) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        Comment comment = new Comment(post, userInfo, request.content());
        saveCreatedComment(post, comment);
        eventPublisher.publishEvent(new CommentChangedEvent.Created(
                postNum,
                null
        ));
        return addResponse(post, comment);
    }

    @Transactional
    public CommentAddResponse addCommentToComment(
            SignUserInfo signUserInfo,
            long postNum,
            CommentToCommentRequest request
    ) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        Comment parent = findComment(request.parentNum());
        Comment comment = new Comment(
                post,
                parent,
                userInfo,
                request.content()
        );
        saveCreatedComment(post, comment);
        eventPublisher.publishEvent(new CommentChangedEvent.Created(
                postNum,
                parent.getCommentNum()
        ));
        return addResponse(post, comment);
    }

    @Transactional
    public CommentResponse updateComment(
            SignUserInfo signUserInfo,
            long commentNum,
            CommentEditRequest request
    ) {
        Comment comment = findOwnedComment(signUserInfo, commentNum);
        commentEditRepository.save(CommentEditRecord.from(comment));
        comment.update(request.content());
        commentRepository.save(comment);
        eventPublisher.publishEvent(
                new CommentChangedEvent.Updated(commentNum)
        );
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(SignUserInfo signUserInfo, long commentNum) {
        Comment comment = findDeletableComment(signUserInfo, commentNum);
        Long parentNum = parentNumOf(comment);
        comment.delete();
        commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentChangedEvent.Deleted(
                commentNum,
                parentNum
        ));
    }

    private void saveCreatedComment(Post post, Comment comment) {
        commentRepository.save(comment);
        postRepository.save(post);
    }

    private CommentAddResponse addResponse(Post post, Comment comment) {
        return new CommentAddResponse(
                post.getCommentCount(),
                CommentResponse.from(comment)
        );
    }

    private UserInfo findUserInfo(long profileId) {
        return userInfoRepository.findByProfileId(profileId)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 유저")
                );
    }

    private Post findPost(long postNum) {
        return postRepository.findByPostNum(postNum)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 게시글")
                );
    }

    private Comment findComment(long commentNum) {
        return commentRepository.findByCommentNum(commentNum)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 댓글")
                );
    }

    private Comment findOwnedComment(
            SignUserInfo signUserInfo,
            long commentNum
    ) {
        Comment comment = findComment(commentNum);
        if (!isOwner(signUserInfo, comment)) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return comment;
    }

    private Comment findDeletableComment(
            SignUserInfo signUserInfo,
            long commentNum
    ) {
        Comment comment = findComment(commentNum);
        if (!isOwner(signUserInfo, comment)
                && signUserInfo.userRole() != UserRole.ADMIN) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return comment;
    }

    private boolean isOwner(SignUserInfo signUserInfo, Comment comment) {
        return comment.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId());
    }

    private Long parentNumOf(Comment comment) {
        return comment.getComment() == null
                ? null
                : comment.getComment().getCommentNum();
    }
}
