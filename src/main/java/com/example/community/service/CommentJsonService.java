package com.example.community.service;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.CommentRepository;
import com.example.community.repository.PostRepository;
import com.example.community.repository.UserRepository;
import com.example.community.util.JWTUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentJsonService implements CommentService{
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    public CommentJsonService(@Qualifier("commentJsonRepository") CommentRepository commentRepository,
                              @Qualifier("postJsonRepository") PostRepository postRepository,
                              @Qualifier("userJsonRepository") UserRepository userRepository,
                              JWTUtil jwtUtil){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void checkUserAuthority(String token, long commentNum) {
        long userNum = jwtUtil.getUidFromToken(token);
        UserRole role = jwtUtil.getRoleFromToken(token);
        CommentDTO comment = commentRepository.getComment(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글", HttpStatus.NOT_FOUND));
        if(comment.getUserNum() != userNum){
            if(role != UserRole.ADMIN) {
                throw new ForbiddenException("작성자만 수정 가능", HttpStatus.FORBIDDEN);
            }
        }
    }

    @Override
    public CommentAddResponse addCommentToPost(String token, long postNum, CommentToPostRequest commentRequest) {
        long userNum = jwtUtil.getUidFromToken(token);

        CommentDTO comment = new CommentDTO();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(postNum);
        comment.setContent(commentRequest.content());
        comment.setUserNum(userNum);
        comment.setParentNum(-1);
        comment.setDepth(0);
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND));

        return new CommentAddResponse(
                postRepository.addComment(postNum)
                ,CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO)));
    }

    @Override
    public CommentAddResponse addCommentToComment(String token, long postNum, CommentToCommentRequest commentRequest) {
        long userNum = jwtUtil.getUidFromToken(token);

        CommentDTO comment = new CommentDTO();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(postNum);
        comment.setContent(commentRequest.content());
        comment.setUserNum(userNum);
        comment.setParentNum(commentRequest.parentNum());
        comment.setDepth(commentRequest.depth());
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND));

        return new CommentAddResponse(
                postRepository.addComment(postNum)
                ,CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO)));
    }

    @Override
    public List<CommentResponse> getPostCommentList(long postNum) {
        List<CommentDTO> comments = commentRepository.getCommentsByPostNum(postNum);
        List<Long> users = comments.stream().map(CommentDTO::getUserNum).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(users);
        Map<Long, UserInfoResponse> userInfoResponseMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(UserInfoDTO::userNum, UserInfoResponse::from));

        return comments.stream()
                .map(c -> CommentResponse.from(c, userInfoResponseMap.get(c.getUserNum()))).toList();
    }

    @Override
    public CommentResponse updateComment(String token, long commentNum, CommentEditRequest commentEditRequest) {
        long userNum = jwtUtil.getUidFromToken(token);
        checkUserAuthority(token, commentNum);
        CommentDTO comment = commentRepository.updateComment(commentNum, commentEditRequest.content())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글", HttpStatus.NOT_FOUND));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저", HttpStatus.NOT_FOUND));

        return CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO));
    }

    @Override
    public void deleteComment(String token, long commentNum) {
        checkUserAuthority(token, commentNum);

        commentRepository.deleteComment(commentNum);
    }
}
