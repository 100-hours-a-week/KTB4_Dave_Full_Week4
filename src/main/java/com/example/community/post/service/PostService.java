package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.entity.PostReport;
import com.example.community.post.entity.PostView;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostReportRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostViewRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.util.ImageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final PostViewRepository postViewRepository;
    private final PostReportRepository postReportRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeRepository userLikeRepository;
    private final ImageConverter imageConverter;

    private Post findPost(long postNum){
        return postRepository.findByPostNum(postNum)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 게시글")
                );
    }

    private UserInfo findUserInfo(long profileId) {
        return userInfoRepository.findByProfileId(profileId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "존재하지 않는 유저"
                        )
                );
    }

    private Post checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        if(!post.getUserInfo().getProfileId().equals(signUserInfo.profileId())
                && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    private Sort getSort(String sort){
        return switch(sort){
            case "likes" -> Sort.by(Sort.Direction.DESC, "post.postState.likeCount");
            case "views" -> Sort.by(Sort.Direction.DESC, "post.postState.viewCount");
            default -> Sort.by(Sort.Direction.DESC, "post.postNum");
        };
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByPage(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, getSort(sort));
        Page<Post> posts = postRepository.findPostByPage(pageable);
        return PostPageResponse.from(posts);
    }

    @Transactional(readOnly = true)
    public PostPageResponse adminGetPostsByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findPostByPage(pageable);

        return PostPageResponse.adminFrom(posts);
    }

    @Transactional(readOnly = true)
    public PostEditPageResponse getPostEditsByPage(long postNum, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        Page<PostEditRecord> postEdits = postEditRepository.findByPost_PostNumOrderByEditIdDesc(postNum, pageable);
        return PostEditPageResponse.from(postEdits);
    }

    @Transactional(readOnly = true)
    public PostEditResponse getPostEdit(long editId){
        PostEditRecord postEditRecord = postEditRepository.findById(editId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 수정 이력"));

        return PostEditResponse.from(postEditRecord);
    }

    @Transactional
    public PostResponse getPost(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        if (post.isBlind()) {
            throw new ForbiddenException("신고 처리된 게시글");
        }

        if (signUserInfo == null || signUserInfo.profileId() == null) {
            return PostResponse.from(post);
        }
        updatePostView(signUserInfo.profileId(), post);

        return PostResponse.from(post);
    }

    private void updatePostView(long profileId, Post post){
        UserInfo userInfo = findUserInfo(profileId);
        postViewRepository
                .findByPost_PostNumAndUserInfo_ProfileId(
                        post.getPostNum(),
                        profileId
                )
                .ifPresentOrElse(
                        PostView::view,
                        () -> postViewRepository.save(
                                new PostView(post, userInfo)
                        )
                );
    }

    @Transactional(readOnly = true)
    public PostResponse adminGetPost(long postNum){
        return PostResponse.adminFrom(findPost(postNum));
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, getSort(sort));
        Page<Post> posts = postRepository.findPostByUserInfo_ProfileId(profileId, pageable);

        return PostPageResponse.from(posts);
    }

    private void recordPostBeforeUpdate(Post post){
        PostEditRecord postEditRecord = PostEditRecord.from(post);
        postEditRepository.save(postEditRecord);
    }

    @Transactional
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) throws IOException {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        String image = imageConverter.updatePostImage(postRequest.image());
        Post post = new Post(userInfo, postRequest.title(),
                postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException {
        Post post = checkUserAuthority(signUserInfo, postNum);
        recordPostBeforeUpdate(post);
        String image = imageConverter.updatePostImage(postRequest.image());
        post.update(postRequest.title(), postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        if(userLikeRepository.existsByUserInfo_ProfileIdAndPost_PostNum(signUserInfo.profileId(), postNum)){
            postUnlike(signUserInfo.profileId(), postNum);
        }
        else{
            postLike(userInfo, post);
        }
        
        return new PostLikeResponse(post.getPostState().getLikeCount());
    }

    private void postUnlike(long profileId, long postNum){
        UserLikePost userLikePost = userLikeRepository.findByUserInfo_ProfileIdAndPost_PostNum(profileId, postNum)
                .orElseThrow(() -> new NotFoundException("좋아요 안 한 게시글"));
        userLikeRepository.delete(userLikePost);
    }

    private void postLike(UserInfo userInfo, Post post){
        UserLikePost userLikePost = new UserLikePost(userInfo, post);
        userLikeRepository.save(userLikePost);
    }

    @Transactional(readOnly = true)
    public boolean isLikePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);

        return userLikeRepository.existsByUserInfo_ProfileIdAndPost_PostNum(userInfo.getProfileId(), post.getPostNum());
    }

    @Transactional
    public PostReportResponse reportPost(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        if(post.getUserInfo().getProfileId().equals(signUserInfo.profileId())){
            throw new BadRequestException("본인이 작성한 글은 신고할 수 없습니다.");
        }
        if(postReportRepository.existsByPost_PostNumAndUserInfo_ProfileId(postNum, userInfo.getProfileId())){
            throw new DuplicateException("이미 신고한 게시글입니다.");
        }
        PostReport postReport = new PostReport(post, userInfo);
        postReportRepository.save(postReport);

        return new PostReportResponse(post.getPostState().getReportCount());
    }

    @Transactional
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        Post post = checkUserAuthority(signUserInfo, postNum);
        post.delete();
    }
}
