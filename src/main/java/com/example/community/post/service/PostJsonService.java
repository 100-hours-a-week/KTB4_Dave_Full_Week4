package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.PostDTO;
import com.example.community.post.dto.PostEditRecordDTO;
import com.example.community.temporaryPost.dto.response.request.PostRequest;
import com.example.community.post.dto.response.*;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.UserLikePostDTO;
import com.example.community.user.entity.UserRole;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.user.repository.UserRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.util.ImageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostJsonService implements PostService{
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;
    private final ImageConverter imageConverter;

    public PostJsonService(@Qualifier("postJsonRepository") PostRepository postRepository,
                           @Qualifier("postEditJsonRepository") PostEditRepository postEditRepository,
                           @Qualifier("userJsonRepository") UserRepository userRepository,
                           @Qualifier("userLikeJsonRepository") UserLikeRepository userLikeRepository,
                           ImageConverter imageConverter){
        this.postRepository = postRepository;
        this.postEditRepository = postEditRepository;
        this.userRepository = userRepository;
        this.userLikeRepository = userLikeRepository;
        this.imageConverter = imageConverter;
    }

    @Override
    public void checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        long userNum = signUserInfo.userNum();
        UserRole role = signUserInfo.userRole();
        PostDTO post = postRepository.getPost(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(userNum != post.getProfileId()){
            if(role != UserRole.ADMIN){
                throw new ForbiddenException("작성자만 수정 가능");
            }
        }
    }


    @Override
    public PostSliceResponse getPostsByPage(int page, int size) {
        Slice<PostDTO> posts = postRepository.getPostsByPage(page, size);
        List<Long> userNums = posts.stream().map(PostDTO::getProfileId).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(userNums)
                .stream().map(ui ->
                        ui.getDeletedAt() != null ? new UserInfoDTO(ui.getUserNum(), ui.getProfileId()
                                , null, "알수없음", null, ui.getUserRole(), ui.getDeletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::getProfileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();

        return new PostSliceResponse(postTitleResponses, posts.hasNext());
    }

    @Override
    public PostResponse getPost(long postNum) {
        postRepository.view(postNum);
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 게시글"));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(post.getProfileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.getDeletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.getUserNum(), userInfoDTO.getProfileId()
                    , null, "알수없음", null, userInfoDTO.getUserRole(), userInfoDTO.getDeletedAt());
        }

        return PostResponse.from(post, userInfoDTO);
    }

    @Override
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size) {
        Page<PostDTO> posts = postRepository.getPostsByProfileId(profileId, page, size +1);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(profileId)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.getDeletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.getUserNum(), userInfoDTO.getProfileId()
                    , null, "알수없음", null, userInfoDTO.getUserRole(), userInfoDTO.getDeletedAt());
        }
        UserInfoDTO finalUserInfoDTO = userInfoDTO;
        List<PostTitleResponse> result = posts.stream().map(p->PostTitleResponse.from(p, UserInfoResponse.from(finalUserInfoDTO))).toList();

        return new PostPageResponse(result, posts.getNumber(), posts.getSize(), posts.getNumberOfElements()
                , posts.getTotalElements(), posts.getTotalPages());
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
                        ui.getDeletedAt() != null ? new UserInfoDTO(ui.getUserNum(), ui.getProfileId()
                                , null, "알수없음", null, ui.getUserRole(), ui.getDeletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::getProfileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = postDTOS.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();

        return new PostPageResponse(postTitleResponses, page, size, userLikePostDTOS.getNumberOfElements(),
                userLikePostDTOS.getTotalElements(), userLikePostDTOS.getTotalPages());
    }

    @Override
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) throws IOException {
        long userNum = signUserInfo.userNum();
        long postNum = postRepository.getPostCount()+1;
        PostDTO post = new PostDTO();
        post.setPostNum(postNum);
        post.setProfileId(userNum);
        post.setTitle(postRequest.title());
        post.setContent(postRequest.content());
        post.setImage(imageConverter.updatePostImage(postRequest.image()));
        post = postRepository.addPost(post);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, userInfoDTO);
    }

    @Override
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException {
        long userNum = signUserInfo.userNum();

        checkUserAuthority(signUserInfo, postNum);
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        postEditRepository.addPostEditRecord(PostEditRecordDTO.from(post));
        post = postRepository.updatePost(postNum, postRequest.title(), postRequest.content(), imageConverter.updatePostImage(postRequest.image()));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, userInfoDTO);
    }

    @Override
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        long userNum = signUserInfo.userNum();

        UserLikePostDTO userLikePost = new UserLikePostDTO(userNum, postNum);
        int like;
        if(userLikeRepository.isUserLikePost(userLikePost)){
            userLikeRepository.deleteUserLikePost(userLikePost);
            like = postRepository.unLike(postNum);
        }
        else{
            userLikeRepository.addUserLikePost(userLikePost);
            like = postRepository.like(postNum);
        }
        return new PostLikeResponse(like);
    }

    @Override
    public boolean isLikePost(SignUserInfo signUserInfo, long postNum) {
        long userNum = signUserInfo.userNum();

        UserLikePostDTO userLikePost = new UserLikePostDTO(userNum, postNum);
        return userLikeRepository.isUserLikePost(userLikePost);
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
