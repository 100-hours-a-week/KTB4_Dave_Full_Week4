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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final PostViewService postViewService;

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

    private Post checkPostOwner(SignUserInfo signUserInfo, long postNum) {
        Post post = findPost(postNum);
        if (!post.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId())) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    private Post checkPostDeleteAuthority(
            SignUserInfo signUserInfo,
            long postNum
    ) {
        Post post = findPost(postNum);
        if (!post.getUserInfo().getProfileId()
                .equals(signUserInfo.profileId())
                && signUserInfo.userRole() != UserRole.ADMIN) {
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    private Sort getSort(String sort){
        return switch(sort){
            case "likes" -> Sort.by(Sort.Direction.DESC, "postState.likeCount")
                    .and(Sort.by(
                            Sort.Direction.DESC,
                            "postNum"
                    ));
            case "views" -> Sort.by(Sort.Direction.DESC, "postState.viewCount")
                    .and(Sort.by(
                            Sort.Direction.DESC,
                            "postNum"
                    ));
            default -> Sort.by(Sort.Direction.DESC, "postNum");
        };
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByPage(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, getSort(sort));
        Page<Post> posts = postRepository.findPostByPage(pageable);
        return PostPageResponse.from(posts);
    }

    @Transactional(readOnly = true)
    public PostPageResponse adminGetPostsByPage(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, getSort(sort));
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
                        (pv) -> {
                            if (pv.view()) {
                                post.view();
                                postViewService.recordView(
                                        post.getPostNum(),
                                        post.getWriteAt()
                                );
                            }
                        },
                        () -> {
                            postViewRepository.save(new PostView(post, userInfo));
                            postViewService.recordView(
                                    post.getPostNum(),
                                    post.getWriteAt()
                            );
                        }
                );
    }

    @Transactional(readOnly = true)
    public PostResponse adminGetPost(long postNum){
        return PostResponse.adminFrom(findPost(postNum));
    }

    @Transactional(readOnly = true)
    public PostPageResponse getMyPosts(
            SignUserInfo signUserInfo,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, getSort(sort));
        Page<Post> posts = postRepository.findPostByUserInfo_ProfileId(signUserInfo.profileId(), pageable);

        return PostPageResponse.from(posts);
    }

    private void recordPostBeforeUpdate(Post post){
        PostEditRecord postEditRecord = PostEditRecord.from(post);
        postEditRepository.save(postEditRecord);
    }

    @Transactional
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        String image = imageConverter.updatePostImage(postRequest.image());
        Post post = new Post(userInfo, postRequest.title(),
                postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) {
        Post post = checkPostOwner(signUserInfo, postNum);
        String image = imageConverter.updatePostImage(postRequest.image());
        recordPostBeforeUpdate(post);
        post.update(postRequest.title(), postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = findUserInfo(signUserInfo.profileId());
        Post post = findPost(postNum);
        Optional<UserLikePost> existingLike = userLikeRepository
                .findByUserInfo_ProfileIdAndPost_PostNum(
                        signUserInfo.profileId(),
                        postNum
                );
        existingLike.ifPresentOrElse(
                this::postUnlike,
                () -> postLike(userInfo, post)
        );
        boolean isLike = existingLike.isEmpty();
        
        return new PostLikeResponse(post.getPostState().getLikeCount(), isLike);
    }

    private void postUnlike(UserLikePost userLikePost){
        userLikePost.getPost().unlike();
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

    @Transactional(readOnly = true)
    public PostSliceResponse getTop10PopularPosts(){
        List<Long> postNums = postViewService.getTop10PopularPostNums();
        Map<Long, Post> postsByPostNum = new HashMap<>();
        postRepository.findPostByPostNumIn(postNums)
                .forEach(post -> postsByPostNum.put(post.getPostNum(), post));
        List<Post> orderedPosts = postNums.stream()
                .map(postsByPostNum::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        Pageable pageable = PageRequest.of(0, 10);
        Slice<Post> postSlice = new SliceImpl<>(orderedPosts, pageable, false);

        return PostSliceResponse.from(postSlice);
    }

    @Transactional
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        Post post = checkPostDeleteAuthority(signUserInfo, postNum);
        post.delete();
    }
}
