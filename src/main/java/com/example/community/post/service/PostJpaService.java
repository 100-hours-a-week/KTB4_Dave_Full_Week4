package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.PostDTO;
import com.example.community.post.dto.PostEditRecordDTO;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostJpaRepository;
import com.example.community.temporaryPost.dto.response.request.PostRequest;
import com.example.community.post.dto.response.*;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.UserLikePostDTO;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.user.repository.UserRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.util.ImageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostJpaService implements PostService{
    private final PostRepository postRepository;
    private final PostJpaRepository postJpaRepository;
    private final PostEditRepository postEditRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeRepository userLikeRepository;
    private final ImageConverter imageConverter;

    public PostJpaService(@Qualifier("postJpaAdapterRepository") PostRepository postRepository,
                          PostJpaRepository postJpaRepository,
                          @Qualifier("postEditJpaAdapterRepository") PostEditRepository postEditRepository,
                          UserInfoRepository userInfoRepository,
                          @Qualifier("userLikeJpaAdapterRepository") UserLikeRepository userLikeRepository,
                          ImageConverter imageConverter){
        this.postRepository = postRepository;
        this.postJpaRepository = postJpaRepository;
        this.postEditRepository = postEditRepository;
        this.userInfoRepository = userInfoRepository;
        this.userLikeRepository = userLikeRepository;
        this.imageConverter = imageConverter;
    }

    @Override
    public void checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(!post.getUserInfo().getProfileId().equals(signUserInfo.profileId()) && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
    }

    @Override
    public PostSliceResponse getPostsByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<Post> posts = postJpaRepository.findPostByPage(pageable);
        List<PostDTO> postDTOS = posts.getContent()
                .stream().map(PostDTO::from).toList();
        Slice<PostDTO> postDTOSlice = new SliceImpl<>(postDTOS, posts.getPageable(), posts.hasNext());
        List<Long> profileIds = postDTOSlice.getContent().stream().map(PostDTO::getProfileId).toList();

        List<UserInfoDTO> userInfoDTOS = userInfoRepository.findByProfileIdIn(profileIds)
                .stream().map(ui ->ui.getDeletedAt() != null ? new UserInfoDTO(ui.getSignInfo().getUserNum(), ui.getProfileId()
                        , null , "알수없음", null, ui.getRole(), ui.getDeletedAt()) : UserInfoDTO.from(ui)).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::getProfileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = postDTOSlice.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();

        return new PostSliceResponse(postTitleResponses, posts.hasNext());
    }

    @Override
    public PostResponse getPost(long postNum) {
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        postRepository.view(postNum);
        UserInfo userInfo = userInfoRepository.findByProfileId(post.getProfileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        return PostResponse.from(post, userInfo);
    }

    @Override
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size) {
        Page<PostDTO> posts = postRepository.getPostsByProfileId(profileId, page, size);
        UserInfo userInfo = userInfoRepository.findByProfileId(profileId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        List<PostTitleResponse> postTitleResponses = posts.stream()
                .map(p ->  PostTitleResponse.from(p, userInfo)).toList();

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
        List<UserInfoDTO> userInfoDTOS = userInfoRepository.findByProfileIdIn(userNums)
                .stream().map(ui ->
                        ui.getDeletedAt() != null ? new UserInfoDTO(ui.getSignInfo().getUserNum(), ui.getProfileId()
                                , null , "알수없음", null, ui.getRole(), ui.getDeletedAt()) : UserInfoDTO.from(ui)).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::getProfileId,
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
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) throws IOException {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        PostDTO postDTO = new PostDTO(signUserInfo.profileId(), postRequest.title(),
                postRequest.content(), image);
        postDTO = postRepository.addPost(postDTO);

        return PostResponse.from(postDTO, userInfo);
    }

    @Override
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        checkUserAuthority(signUserInfo, postNum);
        PostDTO postDTO = postRepository.getPost(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));

        postEditRepository.addPostEditRecord(PostEditRecordDTO.from(postDTO));
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        postDTO = postRepository.updatePost(postNum, postRequest.title(), postRequest.content(),
                image);

        return PostResponse.from(postDTO, userInfo);
    }

    @Override
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        UserLikePostDTO userLikePostDTO = new UserLikePostDTO(userInfo.getProfileId(), postNum);
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
    public boolean isLikePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        UserLikePostDTO userLikePostDTO = new UserLikePostDTO(userInfo.getProfileId(), postNum);
        return userLikeRepository.isUserLikePost(userLikePostDTO);
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
