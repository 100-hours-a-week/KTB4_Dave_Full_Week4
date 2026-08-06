package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.request.TemporaryPostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUpdateResolver;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemporaryPostService {
    private final TemporaryPostRepository temporaryPostRepository;
    private final UserInfoRepository userInfoRepository;
    private final ImageConverter imageConverter;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional(readOnly = true)
    private TemporaryPost checkAuthority(SignUserInfo signUserInfo, long temporaryId){
        TemporaryPost temporaryPost = temporaryPostRepository.findByTemporaryId(temporaryId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 임시저장글"));

        if(!temporaryPost.getUserInfo().getProfileId().equals(signUserInfo.profileId())){
            throw new ForbiddenException("접근권한 부족");
        }
        return temporaryPost;
    }

    @Transactional
    public TemporaryKeyResponse createTemporaryPost(
            SignUserInfo signUserInfo,
            TemporaryPostRequest postRequest
    ) {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        String image = imageConverter.updatePostImage(postRequest.image());
        TemporaryPost temporaryPost = new TemporaryPost(userInfo);
        temporaryPost.update(
                postRequest.title(),
                postRequest.content(),
                image
        );
        temporaryPostRepository.save(temporaryPost);

        return new TemporaryKeyResponse(
                temporaryPost.getTemporaryId(),
                temporaryPost.getImage()
        );
    }

    @Transactional(readOnly = true)
    public TemporaryPostResponse getTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        TemporaryPost temporaryPost = checkAuthority(signUserInfo, temporaryId);

        return TemporaryPostResponse.from(temporaryPost, imageUrlBuilder);
    }

    @Transactional(readOnly = true)
    public List<TemporaryPostTitleResponse> getTemporaryPosts(SignUserInfo signUserInfo){
        return temporaryPostRepository.findByUserInfo_ProfileId(signUserInfo.profileId())
                .stream().map(TemporaryPostTitleResponse::from).toList();
    }

    @Transactional
    public TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostUpdateRequest postRequest) {
        TemporaryPost temporaryPost = checkAuthority(signUserInfo, temporaryId);
        String image = ImageUpdateResolver.resolve(
                temporaryPost.getImage(),
                postRequest.objectKey(),
                postRequest.image(),
                imageConverter::updatePostImage
        );
        temporaryPost.update(postRequest.title(), postRequest.content(), image);

        return TemporaryPostResponse.from(temporaryPost, imageUrlBuilder);
    }

    @Transactional
    public void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        TemporaryPost temporaryPost = checkAuthority(signUserInfo, temporaryId);
        temporaryPostRepository.delete(temporaryPost);
    }
}
