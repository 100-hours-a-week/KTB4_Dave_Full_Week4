package com.example.community.service.post;

import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.post.PostEditRecordDTO;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserLikePostDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.post.PostEditRepository;
import com.example.community.repository.post.PostRepository;
import com.example.community.repository.user.UserLikeRepository;
import com.example.community.repository.user.UserRepository;
import com.example.community.resolver.SignUserInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostJpaService implements PostService{
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;

    public PostJpaService(@Qualifier("postJpaAdapterRepository") PostRepository postRepository,
                          @Qualifier("postEditJpaAdapterRepository") PostEditRepository postEditRepository,
                          @Qualifier("userJpaRepository") UserRepository userRepository,
                          @Qualifier("userLikeJpaAdapterRepository") UserLikeRepository userLikeRepository){
        this.postRepository = postRepository;
        this.postEditRepository = postEditRepository;
        this.userRepository = userRepository;
        this.userLikeRepository = userLikeRepository;
    }

    @Override
    public void checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        PostDTO post = postRepository.getPost(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(!post.getProfileId().equals(signUserInfo.profileId()) && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
    }

    @Override
    public PostSliceResponse getPostsByPage(int page, int size) {
        Slice<PostDTO> posts = postRepository.getPostsByPage(page, size);
        List<Long> profileIds = posts.getContent().stream().map(PostDTO::getProfileId).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(profileIds)
                .stream().map(ui ->ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                        , "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::profileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();

        return new PostSliceResponse(postTitleResponses, posts.hasNext());
    }

    @Override
    public PostResponse getPost(long postNum) {
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        postRepository.view(postNum);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(post.getProfileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, userInfoDTO);
    }

    @Override
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size) {
        Page<PostDTO> posts = postRepository.getPostsByProfileId(profileId, page, size);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(profileId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        List<PostTitleResponse> postTitleResponses = posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoDTO)).toList();

        return new PostPageResponse(postTitleResponses, posts.getNumber(), posts.getSize()
        , posts.getNumberOfElements(), posts.getTotalElements(), posts.getTotalPages());
    }

    @Override
    public PostPageResponse getLikePosts(long profileId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserLikePostDTO> userLikePostDTOS = userLikeRepository.getUserLikePosts(profileId, pageable);
        List<Long> postNums = userLikePostDTOS.getContent().stream().map(UserLikePostDTO::getPostNum).toList();
        List<PostDTO> postDTOS = postRepository.getPosts(postNums);
        List<Long> userNums = postDTOS.stream().map(PostDTO::getProfileId).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(userNums)
                .stream().map(ui ->
                        ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                                , "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::profileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = postDTOS.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();


        return new PostPageResponse(
                postTitleResponses,
                page,
                size,
                userLikePostDTOS.getNumberOfElements(),
                userLikePostDTOS.getTotalElements(),
                userLikePostDTOS.getTotalPages()
        );
    }

    @Override
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) {
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        PostDTO postDTO = new PostDTO(signUserInfo.profileId(), postRequest.title(),
                postRequest.content(), postRequest.image());
        postDTO = postRepository.addPost(postDTO);

        return PostResponse.from(postDTO, userInfoDTO);
    }

    @Override
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) {
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        checkUserAuthority(signUserInfo, postNum);
        PostDTO postDTO = postRepository.getPost(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));

        postEditRepository.addPostEditRecord(PostEditRecordDTO.from(postDTO));
        postDTO = postRepository.updatePost(postNum, postRequest.title(), postRequest.content(),
                postRequest.image());

        return PostResponse.from(postDTO, userInfoDTO);
    }

    @Override
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        UserLikePostDTO userLikePostDTO = new UserLikePostDTO(userInfoDTO.profileId(), postNum);
        int likeCount;
        if(userLikeRepository.isUserLikePost(userLikePostDTO)){
            userLikeRepository.deleteUserLikePost(userLikePostDTO);
            likeCount = postRepository.unLike(postNum);
        }else{
            userLikeRepository.addUserLikePost(userLikePostDTO);
            likeCount = postRepository.like(postNum);
        }

        return new PostLikeResponse(likeCount);
    }

    @Override
    public PostReportResponse reportPost(long postNum) {
        return new PostReportResponse(postRepository.reportPost(postNum));
    }

    @Override
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        checkUserAuthority(signUserInfo, postNum);
        postRepository.deletePost(postNum);
    }
}
