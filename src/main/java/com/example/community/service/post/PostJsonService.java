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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                        ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                                , null, "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
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
        postRepository.view(postNum);
        PostDTO post = postRepository.getPost(postNum)
                .orElseThrow(()->new NotFoundException("존재하지 않는 게시글"));
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(post.getProfileId())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.deletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.userNum(), userInfoDTO.profileId()
                    , null, "알수없음", null, userInfoDTO.userRole(), userInfoDTO.deletedAt());
        }

        return PostResponse.from(post, userInfoDTO);
    }

    @Override
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size) {
        Page<PostDTO> posts = postRepository.getPostsByProfileId(profileId, page, size +1);
        UserInfoDTO userInfoDTO = userRepository.getUserInfo(profileId)
                .orElseThrow(()->new NotFoundException("존재하지 않는 유저"));
        if(userInfoDTO.deletedAt() != null){
            userInfoDTO = new UserInfoDTO(userInfoDTO.userNum(), userInfoDTO.profileId()
                    , null, "알수없음", null, userInfoDTO.userRole(), userInfoDTO.deletedAt());
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
                        ui.deletedAt() != null ? new UserInfoDTO(ui.userNum(), ui.profileId()
                                , null, "알수없음", null, ui.userRole(), ui.deletedAt()) : ui).toList();
        Map<Long, UserInfoResponse> userInfoMap = userInfoDTOS.stream()
                .collect(Collectors.toMap(
                        UserInfoDTO::profileId,
                        UserInfoResponse::from
                ));
        List<PostTitleResponse> postTitleResponses = postDTOS.stream()
                .map(p ->  PostTitleResponse.from(p, userInfoMap.get(p.getProfileId()))).toList();

        return new PostPageResponse(postTitleResponses, page, size, userLikePostDTOS.getNumberOfElements(),
                userLikePostDTOS.getTotalElements(), userLikePostDTOS.getTotalPages());
    }

    public String updatePostImage(MultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get(System.getProperty("user.dir"), "app", "uploads", "posts");
        Path targetPath = uploadPath.resolve(storedFileName);

        file.transferTo(targetPath);

        String imageUrl = "/images/posts/" + storedFileName;

        return imageUrl;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("파일명이 비어 있습니다.");
        }

        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        return originalFilename.substring(dotIndex + 1).toLowerCase();
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
        post.setImage(updatePostImage(postRequest.image()));
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
        post = postRepository.updatePost(postNum, postRequest.title(), postRequest.content(), updatePostImage(postRequest.image()));
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
