package com.example.community.service.comment;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.comment.CommentEditRecord;
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
import com.example.community.repository.comment.CommentEditJpaAdapterRepository;
import com.example.community.repository.comment.CommentEditJpaRepository;
import com.example.community.repository.comment.CommentRepository;
import com.example.community.repository.post.PostRepository;
import com.example.community.repository.user.UserRepository;
import com.example.community.resolver.SignUserInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentJpaService implements CommentService{
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentEditJpaAdapterRepository commentEditJpaAdapterRepository;

    public CommentJpaService(@Qualifier("commentJpaAdapterRepository") CommentRepository commentRepository,
                             @Qualifier("postJpaAdapterRepository") PostRepository postRepository,
                             @Qualifier("userJpaRepository") UserRepository userRepository,
                             CommentEditJpaAdapterRepository commentEditJpaAdapterRepository){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentEditJpaAdapterRepository = commentEditJpaAdapterRepository;
    }

    @Override
    public void checkUserAuthority(SignUserInfo signUserInfo, long commentNum) {
        CommentDTO commentDTO = commentRepository.getComment(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        if(commentDTO.getCommentNum() != signUserInfo.profileId() && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
    }

    @Override
    public CommentAddResponse addCommentToPost(SignUserInfo signUserInfo, long postNum, CommentToPostRequest commentRequest) {
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        postRepository.getPost(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        CommentDTO commentDTO = new CommentDTO(postNum, signUserInfo.profileId(), commentRequest.content());
        commentDTO = commentRepository.addComment(commentDTO);

        return new CommentAddResponse(postRepository.addComment(postNum), CommentResponse.of(commentDTO, userInfoDTO));
    }

    @Override
    public CommentAddResponse addCommentToComment(SignUserInfo signUserInfo, long postNum, CommentToCommentRequest commentRequest) {
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        postRepository.getPost(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        CommentDTO parentComment = commentRepository.getComment(commentRequest.parentNum())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));
        CommentDTO commentDTO = new CommentDTO(postNum, parentComment.getCommentNum(), parentComment.getDepth()+1
                , signUserInfo.profileId(), commentRequest.content());
        commentDTO = commentRepository.addComment(commentDTO);

        return new CommentAddResponse(postRepository.addComment(postNum), CommentResponse.of(commentDTO, userInfoDTO));

    }

    @Override
    public List<CommentListResponse> getPostCommentList(long postNum) {
        List<CommentDTO> commentDTOS = commentRepository.getCommentsByPostNum(postNum);
        List<Long> users = commentDTOS.stream().map(CommentDTO::getProfileId).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(users)
                .stream().map(ui -> ui.deletedAt() != null ?
                        new UserInfoDTO(ui.userNum(), ui.profileId()
                                , "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoResponseMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(UserInfoDTO::profileId, UserInfoResponse::from));

        List<CommentResponse> commentResponses = commentDTOS.stream()
                .map(c -> CommentResponse.of(c, userInfoResponseMap.get(c.getProfileId()))).toList();

        return  getCommentListResponse(commentResponses);
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
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        checkUserAuthority(signUserInfo, commentNum);
        CommentDTO commentDTO = commentRepository.getComment(commentNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글"));

        commentEditJpaAdapterRepository.addCommentEdit(commentDTO);
        commentDTO = commentRepository.updateComment(commentNum, commentEditRequest.content());

        return CommentResponse.of(commentDTO, userInfoDTO);
    }

    @Override
    public void deleteComment(SignUserInfo signUserInfo, long commentNum) {
        checkUserAuthority(signUserInfo, commentNum);
        commentRepository.deleteComment(commentNum);
    }
}
