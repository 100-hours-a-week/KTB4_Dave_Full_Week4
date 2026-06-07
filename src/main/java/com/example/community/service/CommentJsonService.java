package com.example.community.service;

import com.example.community.domain.comment.Comment;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentListResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.domain.token.Token;
import com.example.community.domain.user.User;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.CommentRepository;
import com.example.community.repository.PostRepository;
import com.example.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentJsonService implements CommentService{
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentJsonService(@Qualifier("commentJsonRepository") CommentRepository commentRepository,
                              @Qualifier("postJsonRepository") PostRepository postRepository,
                              @Qualifier("userJsonRepository") UserRepository userRepository){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void checkUserAuthority(Token token, long commentNum) {
        Comment comment = commentRepository.getComment(commentNum).orElseThrow(() -> new RuntimeException("존재하지 않는 댓글"));
        if(comment.getUserNum() != token.userNum()){
            if(token.role() != UserRole.ADMIN) {
                throw new RuntimeException("작성자만 수정 가능");
            }
        }
    }

    @Override
    public CommentAddResponse addCommentToPost(Token token, CommentToPostRequest commentRequest) {
        Comment comment = new Comment();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(commentRequest.postNum());
        comment.setContent(commentRequest.content());
        comment.setUserNum(token.userNum());
        comment.setParentNum(-1);
        comment.setDepth(0);
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(token.userNum()).orElseThrow(()->new RuntimeException("존재하지 않는 유저"));

        return new CommentAddResponse(
                postRepository.addComment(commentRequest.postNum())
                ,CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO)));
    }

    @Override
    public CommentAddResponse addCommentToComment(Token token, CommentToCommentRequest commentRequest) {
        Comment comment = new Comment();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(commentRequest.postNum());
        comment.setContent(commentRequest.content());
        comment.setUserNum(token.userNum());
        comment.setParentNum(commentRequest.parentNum());
        comment.setDepth(commentRequest.depth());
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(token.userNum()).orElseThrow(()->new RuntimeException("존재하지 않는 유저"));

        return new CommentAddResponse(
                postRepository.addComment(commentRequest.postNum())
                ,CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO)));
    }

    @Override
    public List<CommentListResponse> getPostCommentList(long postNum) {
        List<Comment> comments = commentRepository.getCommentsByPostNum(postNum);
        List<Long> users = comments.stream().map(Comment::getUserNum).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(users);
        Map<Long, UserInfoResponse> userInfoResponseMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(UserInfoDTO::userNum, UserInfoResponse::from));

        List<CommentResponse> commentResponses = comments.stream()
                .map(c -> CommentResponse.from(c, userInfoResponseMap.get(c.getUserNum()))).toList();



        return List.of();
    }

    @Override
    public CommentResponse updateComment(Token token, CommentEditRequest commentEditRequest) {
        checkUserAuthority(token, commentEditRequest.commentNum());
        Comment comment = commentRepository.updateComment(commentEditRequest.commentNum(), commentEditRequest.content()).orElseThrow(() -> new RuntimeException("존재하지 않는 댓글"));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(token.userNum()).orElseThrow(()->new RuntimeException("존재하지 않는 유저"));

        return CommentResponse.from(comment, UserInfoResponse.from(userInfoDTO));
    }

    @Override
    public void deleteComment(Token token, long commentNum) {
        checkUserAuthority(token, commentNum);

        commentRepository.deleteComment(commentNum);
    }
}
