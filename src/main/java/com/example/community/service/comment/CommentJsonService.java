package com.example.community.service.comment;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.comment.request.CommentEditRequest;
import com.example.community.domain.comment.request.CommentToCommentRequest;
import com.example.community.domain.comment.request.CommentToPostRequest;
import com.example.community.domain.comment.response.CommentAddResponse;
import com.example.community.domain.comment.response.CommentListResponse;
import com.example.community.domain.comment.response.CommentResponse;
import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.comment.CommentRepository;
import com.example.community.repository.post.PostRepository;
import com.example.community.repository.user.UserRepository;
import com.example.community.resolver.SignUserInfo;
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
    public void checkUserAuthority(SignUserInfo signUserInfo, long commentNum) {
        long userNum = signUserInfo.userNum();
        UserRole role = signUserInfo.userRole();
        CommentDTO comment = commentRepository.getComment(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        if(comment.getProfileId() != userNum){
            if(role != UserRole.ADMIN) {
                throw new ForbiddenException("작성자만 수정 가능");
            }
        }
    }

    @Override
    public CommentAddResponse addCommentToPost(SignUserInfo signUserInfo, long postNum, CommentToPostRequest commentRequest) {
        long userNum = signUserInfo.userNum();

        CommentDTO comment = new CommentDTO();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(postNum);
        comment.setContent(commentRequest.content());
        comment.setProfileId(userNum);
        comment.setParentNum(null);
        comment.setDepth(0);
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));

        return new CommentAddResponse(
                postRepository.addComment(postNum)
                ,CommentResponse.of(comment, userInfoDTO));
    }

    @Override
    public CommentAddResponse addCommentToComment(SignUserInfo signUserInfo, long postNum, CommentToCommentRequest commentRequest) {
        long userNum = signUserInfo.userNum();

        CommentDTO parent = commentRepository.getComment(commentRequest.parentNum())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        CommentDTO comment = new CommentDTO();
        long commentNum = commentRepository.getCommentCount()+1;
        comment.setCommentNum(commentNum);
        comment.setPostNum(postNum);
        comment.setContent(commentRequest.content());
        comment.setProfileId(userNum);
        comment.setParentNum(commentRequest.parentNum());
        comment.setDepth(parent.getDepth()+1);
        comment = commentRepository.addComment(comment);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));

        return new CommentAddResponse(
                postRepository.addComment(postNum)
                ,CommentResponse.of(comment, userInfoDTO));
    }

    @Override
    public List<CommentListResponse> getPostCommentList(long postNum) {
        List<CommentDTO> comments = commentRepository.getCommentsByPostNum(postNum);
        List<Long> users = comments.stream().map(CommentDTO::getProfileId).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(users);
        Map<Long, UserInfoResponse> userInfoResponseMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(UserInfoDTO::profileId, UserInfoResponse::from));

        return getCommentListResponse(
                comments.stream()
                        .map(c ->
                                CommentResponse.of(
                                        c, userInfoResponseMap.get(c.getProfileId())
                                )
                        ).toList()
                );
    }
    private List<CommentListResponse> getCommentListResponse(List<CommentResponse> commentResponses){
        List<CommentResponse> zeroDepth = commentResponses.stream().filter(c -> c.depth() == 0).toList();
        List<CommentResponse> firstDepth = commentResponses.stream().filter(c -> c.depth() == 1).toList();
        List<CommentResponse> secondDepth = commentResponses.stream().filter(c -> c.depth() == 2).toList();
        List<CommentResponse> thirdDepth = commentResponses.stream().filter(c -> c.depth() == 3).toList();
        List<CommentListResponse> result = zeroDepth.stream().map(CommentListResponse::of).toList();

        Map<Long, List<CommentListResponse>> firstChild = firstDepth.stream()
                .collect(Collectors.groupingBy(CommentResponse::commentNum,
                        Collectors.mapping(CommentListResponse::of, Collectors.toList())));
        for(CommentListResponse commentListResponse: result){
            commentListResponse.addChild(
                    firstChild.getOrDefault(
                            commentListResponse.getComment().commentNum(),
                            List.of()
                    )
            );
        }

        Map<Long, List<CommentListResponse>> secondChild = secondDepth.stream()
                .collect(Collectors.groupingBy(CommentResponse::commentNum,
                        Collectors.mapping(CommentListResponse::of, Collectors.toList())));
        List<CommentListResponse> firstChildList = firstChild.values().stream().flatMap(List::stream).toList();
        for(CommentListResponse commentListResponse: firstChildList){
            commentListResponse.addChild(
                    secondChild.getOrDefault(
                            commentListResponse.getComment().commentNum(),
                            List.of()
                    )
            );
        }

        Map<Long, List<CommentListResponse>> thirdChild = thirdDepth.stream()
                .collect(Collectors.groupingBy(CommentResponse::commentNum,
                        Collectors.mapping(CommentListResponse::of, Collectors.toList())));
        List<CommentListResponse> secondChildList = secondChild.values().stream().flatMap(List::stream).toList();
        for(CommentListResponse commentListResponse: secondChildList){
            commentListResponse.addChild(
                    thirdChild.getOrDefault(
                            commentListResponse.getComment().commentNum(),
                            List.of()
                    )
            );
        }
        return result;
    }

    @Override
    public CommentResponse updateComment(SignUserInfo signUserInfo, long commentNum, CommentEditRequest commentEditRequest) {
        long userNum = signUserInfo.userNum();
        checkUserAuthority(signUserInfo, commentNum);
        CommentDTO comment = commentRepository.updateComment(commentNum, commentEditRequest.content());
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));

        return CommentResponse.of(comment, userInfoDTO);
    }

    @Override
    public void deleteComment(SignUserInfo signUserInfo, long commentNum) {
        checkUserAuthority(signUserInfo, commentNum);

        commentRepository.deleteComment(commentNum);
    }
}
