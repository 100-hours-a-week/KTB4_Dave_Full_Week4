package com.example.community.service;

import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.post.PostEditRecordDTO;
import com.example.community.domain.post.request.PostEditRequest;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.*;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserLikePostDTO;
import com.example.community.domain.user.UserRole;
import com.example.community.domain.user.response.UserInfoResponse;
import com.example.community.repository.PostEditRepository;
import com.example.community.repository.PostRepository;
import com.example.community.repository.UserLikeRepository;
import com.example.community.repository.UserRepository;
import com.example.community.resolver.SignUserInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostJsonService implements PostService{
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;

    public PostJsonService(@Qualifier("postJsonRepository") PostRepository postRepository,
                           @Qualifier("postEditJsonRepository") PostEditRepository postEditRepository,
                           @Qualifier("userJsonRepository") UserRepository userRepository,
                           @Qualifier("userLikeJsonRepository") UserLikeRepository userLikeRepository){
        this.postRepository = postRepository;
        this.postEditRepository = postEditRepository;
        this.userRepository = userRepository;
        this.userLikeRepository = userLikeRepository;
    }

    @Override
    public void checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        long userNum = signUserInfo.userNum();
        UserRole role = signUserInfo.userRole();
        PostDTO post = postRepository.getPost(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(userNum != post.getUserNum()){
            if(role != UserRole.ADMIN){
                throw new ForbiddenException("작성자만 수정 가능", HttpStatus.FORBIDDEN);
            }
        }
    }

    @Override
    public List<PostTitleResponse> getAllPosts() {
        List<PostDTO> posts = postRepository.getAllPosts();

        List<Long> userNums = posts.stream().map(PostDTO::getUserNum).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(userNums)
                .stream().map(ui ->
                        ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                                , "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::profileId,
                        UserInfoResponse::from
                ));

        return posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getUserNum()))).toList();
    }

    @Override
    public PostListResponse getPostsByPage(int index, int offset) {
        List<PostDTO> posts = postRepository.getPostsByPage(index, offset+1);
        boolean hasNext = posts.size() > offset;
        if(hasNext) {
            posts.removeLast();
        }
        List<Long> userNums = posts.stream().map(PostDTO::getUserNum).toList();
        List<UserInfoDTO> userInfoDTOS = userRepository.getUserInfos(userNums)
                .stream().map(ui ->
                        ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                                , "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::profileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getUserNum()))).toList();

        return new PostListResponse(postTitleResponses, hasNext);
    }

    @Override
    public PostResponse getPost(long postNum) {
        postRepository.view(postNum);
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 게시글"));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(post.getUserNum())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.deletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.userNum(), userInfoDTO.profileId()
                    , "알수없음", null, userInfoDTO.userRole(), userInfoDTO.deletedAt());
        }

        return PostResponse.from(post, UserInfoResponse.from(userInfoDTO));
    }

    @Override
    public PostListResponse getPostsByUserNum(long userNum, int index, int offset) {
        List<PostDTO> posts = postRepository.getPostsByUserNum(userNum, index, offset+1);
        boolean hasNext = posts.size() > offset;
        if(hasNext){
            posts.removeLast();
        }
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.deletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.userNum(), userInfoDTO.profileId()
                    , "알수없음", null, userInfoDTO.userRole(), userInfoDTO.deletedAt());
        }
        UserInfoDTO finalUserInfoDTO = userInfoDTO;
        List<PostTitleResponse> result = posts.stream().map(p->PostTitleResponse.from(p, UserInfoResponse.from(finalUserInfoDTO))).toList();

        return new PostListResponse(result, hasNext);
    }


    @Override
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) {
        long userNum = signUserInfo.userNum();
        long postNum = postRepository.getPostCount()+1;
        PostDTO post = new PostDTO();
        post.setPostNum(postNum);
        post.setUserNum(userNum);
        post.setTitle(postRequest.title());
        post.setContent(postRequest.content());
        post.setImage(postRequest.image());
        post = postRepository.addPost(post);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, UserInfoResponse.from(userInfoDTO));
    }

    @Override
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostEditRequest postEditRequest) {
        long userNum = signUserInfo.userNum();

        checkUserAuthority(signUserInfo, postNum);
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 게시글"));
        postEditRepository.addPostEditRecord(PostEditRecordDTO.from(post));
        post = postRepository.updatePost(postNum, postEditRequest.title(), postEditRequest.content(), postEditRequest.image());
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, UserInfoResponse.from(userInfoDTO));
    }

    @Override
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        long userNum = signUserInfo.userNum();

        UserLikePostDTO userLikePost = new UserLikePostDTO();
        userLikePost.setUserNum(userNum);
        userLikePost.setPostNum(postNum);
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
    public PostReportResponse reportPost(long postNum) {
        return new PostReportResponse(postRepository.reportPost(postNum));
    }


    @Override
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        checkUserAuthority(signUserInfo, postNum);
        postRepository.deletePost(postNum);
    }
}
