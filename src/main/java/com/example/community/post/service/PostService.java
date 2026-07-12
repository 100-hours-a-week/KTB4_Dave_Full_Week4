package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.util.ImageConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeRepository userLikeRepository;
    private final ImageConverter imageConverter;

    public PostService(PostRepository postRepository,
                       PostEditRepository postEditRepository,
                       UserInfoRepository userInfoRepository,
                       UserLikeRepository userLikeRepository,
                       ImageConverter imageConverter){
        this.postRepository = postRepository;
        this.postEditRepository = postEditRepository;
        this.userInfoRepository = userInfoRepository;
        this.userLikeRepository = userLikeRepository;
        this.imageConverter = imageConverter;
    }

    @Transactional(readOnly = true)
    private Post checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        Post post = postRepository.findByPostNum(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(!post.getUserInfo().getProfileId().equals(signUserInfo.profileId()) && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByPage(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        if(sort.equals("likes")) {
            posts = postRepository.findPostByPageOrderByLikeCount(pageable);
        }
        else if(sort.equals("views")){
            posts = postRepository.findPostByPageOrderByViewCount(pageable);
        }
        else{
            posts = postRepository.findPostByPage(pageable);
        }

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
    public PostResponse getPost(long postNum) {
        Post post = postRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(post.isBlind()){
            throw new ForbiddenException("신고 처리된 게시글");
        }
        post.view();
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PostResponse adminGetPost(long postNum){
        Post post = postRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));

        return PostResponse.from(post);
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        if(sort.equals("likes")) {
            posts = postRepository.findByUserInfo_ProfileIdOrderByLikeCountDesc(profileId, pageable);
        }
        else if(sort.equals("views")){
            posts = postRepository.findByUserInfo_ProfileIdOrderByViewCountDesc(profileId, pageable);
        }
        else{
            posts = postRepository.findByUserInfo_ProfileIdOrderByPostNumDesc(profileId, pageable);
        }

        return PostPageResponse.from(posts);
    }

    @Transactional
    public PostResponse addPost(SignUserInfo signUserInfo, PostRequest postRequest) throws IOException {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        Post post = new Post(userInfo, postRequest.title(),
                postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException {
        Post post = checkUserAuthority(signUserInfo, postNum);
        PostEditRecord postEditRecord = PostEditRecord.from(post);
        postEditRepository.save(postEditRecord);
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        post.update(postRequest.title(), postRequest.content(), image);
        postRepository.save(post);

        return PostResponse.from(post);
    }

    @Transactional
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        Post post = postRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        int likeCount;
        try {
            UserLikePost userLikePost = userLikeRepository.findByUserInfo_ProfileIdAndPost_PostNum(signUserInfo.profileId(), postNum)
                    .orElseThrow();
            likeCount = userLikePost.getPost().unlike();
            userLikeRepository.delete(userLikePost);
        }
        catch(Exception e){
            UserLikePost userLikePost = new UserLikePost(userInfo, post);
            likeCount = userLikePost.getPost().like();
            userLikeRepository.save(userLikePost);
        }
        return new PostLikeResponse(likeCount);
    }

    @Transactional(readOnly = true)
    public boolean isLikePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        // 여기에 게시글도 검증해야 하지 않나 싶음
        Post post = postRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));

        return userLikeRepository.existsByUserInfo_ProfileIdAndPost_PostNum(userInfo.getProfileId(), post.getPostNum());
    }

    @Transactional
    public PostReportResponse reportPost(SignUserInfo signUserInfo, long postNum) {
        Post post = checkUserAuthority(signUserInfo, postNum);
        return new PostReportResponse(post.report());
    }

    @Transactional
    public void deletePost(SignUserInfo signUserInfo, long postNum) {
        Post post = checkUserAuthority(signUserInfo, postNum);
        post.delete();
    }
}
