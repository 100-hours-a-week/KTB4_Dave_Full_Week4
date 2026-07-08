package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.repository.PostEditJpaRepository;
import com.example.community.post.repository.PostJpaRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.response.request.PostRequest;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeJpaRepository;
import com.example.community.util.ImageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class PostJpaService implements PostService{
    private final PostRepository postRepository;
    private final PostJpaRepository postJpaRepository;
    private final PostEditJpaRepository postEditJpaRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeJpaRepository userLikeJpaRepository;
    private final ImageConverter imageConverter;

    public PostJpaService(@Qualifier("postJpaAdapterRepository") PostRepository postRepository,
                          PostJpaRepository postJpaRepository,
                          PostEditJpaRepository postEditJpaRepository,
                          UserInfoRepository userInfoRepository,
                          UserLikeJpaRepository userLikeJpaRepository,
                          ImageConverter imageConverter){
        this.postRepository = postRepository;
        this.postJpaRepository = postJpaRepository;
        this.postEditJpaRepository = postEditJpaRepository;
        this.userInfoRepository = userInfoRepository;
        this.userLikeJpaRepository = userLikeJpaRepository;
        this.imageConverter = imageConverter;
    }

    private Post checkUserAuthority(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        Post post = postJpaRepository.findByPostNum(postNum).orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        if(!post.getUserInfo().getProfileId().equals(userInfo.getProfileId()) && userInfo.getRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근 권한 부족");
        }
        return post;
    }

    @Override
    public PostPageResponse getPostsByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postJpaRepository.findPostByPage(pageable);
        List<PostTitleResponse> postTitleResponses = posts.getContent().stream()
                .map(PostTitleResponse::from).toList();

        return PostPageResponse.from(posts);
    }

    @Override
    public PostResponse getPost(long postNum) {
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        post.view();
        postJpaRepository.save(post);

        return PostResponse.from(post);
    }

    @Override
    public PostPageResponse getPostsByProfileId(long profileId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postJpaRepository.findByUserInfo_ProfileIdOrderByPostNumDesc(profileId, pageable);
        List<PostTitleResponse> postTitleResponses = posts.getContent().stream()
                .map(PostTitleResponse::from).toList();

        return new PostPageResponse(postTitleResponses, posts.getNumber(), posts.getSize()
        , posts.getNumberOfElements(), posts.getTotalElements(), posts.getTotalPages());
    }

    @Override
    public PostPageResponse getLikePosts(long profileId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserLikePost> userLikePosts = userLikeJpaRepository.findByUserInfo_ProfileId(profileId, pageable);
        List<PostTitleResponse> postTitleResponses = userLikePosts.stream()
                .map(PostTitleResponse::from).toList();

        return new PostPageResponse(
                postTitleResponses,
                page,
                size,
                userLikePosts.getNumberOfElements(),
                userLikePosts.getTotalElements(),
                userLikePosts.getTotalPages()
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
        Post post = new Post(userInfo, postRequest.title(),
                postRequest.content(), image);
        postJpaRepository.save(post);

        return PostResponse.from(post);
    }

    @Override
    public PostResponse updatePost(SignUserInfo signUserInfo, long postNum, PostRequest postRequest) throws IOException {
        Post post = checkUserAuthority(signUserInfo, postNum);
        PostEditRecord postEditRecord = PostEditRecord.from(post);
        postEditJpaRepository.save(postEditRecord);
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        post.update(postRequest.title(), postRequest.content(), image);
        postJpaRepository.save(post);

        return PostResponse.from(post);
    }

    @Override
    public PostLikeResponse likePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        Post post = postJpaRepository.findByPostNum(postNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글"));
        int likeCount;
        try {
            UserLikePost userLikePost = userLikeJpaRepository.findByUserInfo_ProfileIdAndPost_PostNum(signUserInfo.profileId(), postNum)
                    .orElseThrow();
            userLikeJpaRepository.delete(userLikePost);
            likeCount = postRepository.unLike(postNum);
        }
        catch(Exception e){
            UserLikePost userLikePost = new UserLikePost(userInfo, post);
            userLikeJpaRepository.save(userLikePost);
            likeCount = postRepository.like(postNum);
        }

        return new PostLikeResponse(likeCount);
    }

    @Override
    public boolean isLikePost(SignUserInfo signUserInfo, long postNum) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        // 여기에 게시글도 검증해야 하지 않나 싶음

        return userLikeJpaRepository.existsByUserInfo_ProfileIdAndPost_PostNum(userInfo.getProfileId(), postNum);
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
