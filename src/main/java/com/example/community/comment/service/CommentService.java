package com.example.community.comment.service;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService{
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserInfoRepository userInfoRepository;
    private final CommentEditRepository commentEditRepository;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserInfoRepository userInfoRepository,
                          CommentEditRepository commentEditRepository){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userInfoRepository = userInfoRepository;
        this.commentEditRepository = commentEditRepository;
    }

    @Transactional(readOnly = true)
    private Comment checkUserAuthority(SignUserInfo signUserInfo, long commentNum) {
        Comment comment = commentRepository.findByCommentNum(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        if(!comment.getUserInfo().getProfileId().equals(signUserInfo.profileId()) && signUserInfo.userRole().equals(UserRole.ADMIN)){
            throw new ForbiddenException("접근 권한 부족");
        }
        return comment;
    }

    @Transactional
    public CommentAddResponse addCommentToPost(SignUserInfo signUserInfo, long postNum, CommentToPostRequest commentRequest) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        Post post = postRepository.findByPostNum(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        Comment comment = new Comment(post, userInfo, commentRequest.content());
        commentRepository.save(comment);
        postRepository.save(post);

        return new CommentAddResponse(post.getCommentCount(), CommentResponse.from(comment));
    }

    @Transactional
    public CommentAddResponse addCommentToComment(SignUserInfo signUserInfo, long postNum, CommentToCommentRequest commentRequest) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        Post post = postRepository.findByPostNum(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        Comment parent = commentRepository.findByCommentNum(commentRequest.parentNum())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 댓글"));
        Comment comment = new Comment(post, parent, userInfo, commentRequest.content());
        commentRepository.save(comment);
        postRepository.save(post);

        return new CommentAddResponse(post.getCommentCount(), CommentResponse.from(comment));

    }

    @Transactional(readOnly = true)
    public CommentPageResponse getPostCommentPage(long postNum, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPost_postNum(postNum, pageRequest);
        return CommentPageResponse.from(comments);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse adminGetPostCommentPage(long postNum, int page, int size){
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPost_postNum(postNum, pageRequest);
        return CommentPageResponse.adminFrom(comments);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getChildCommentPage(long commentNum, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByParentNum(commentNum, pageRequest);
        return CommentPageResponse.from(comments);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse adminGetChildCommentPage(long commentNum, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByParentNum(commentNum, pageRequest);
        return CommentPageResponse.adminFrom(comments);
    }

    @Transactional(readOnly = true)
    public CommentEditPageResponse getCommentEditsByPage(long commentNum, int page, int size){
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CommentEditRecord> comments = commentEditRepository.findByComment_CommentNumOrderByEditIdDesc(commentNum, pageRequest);
        return CommentEditPageResponse.from(comments);
    }

    @Transactional
    public CommentResponse updateComment(SignUserInfo signUserInfo, long commentNum, CommentEditRequest commentEditRequest) {
        Comment comment = checkUserAuthority(signUserInfo, commentNum);
        CommentEditRecord commentEditRecord = CommentEditRecord.from(comment);

        commentEditRepository.save(commentEditRecord);
        comment.update(commentEditRequest.content());
        commentRepository.save(comment);

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(SignUserInfo signUserInfo, long commentNum) {
        Comment comment = checkUserAuthority(signUserInfo, commentNum);
        comment.delete();
        commentRepository.save(comment);
    }
}
