package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostJpaRepository;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemporaryPostService {
    private final TemporaryPostJpaRepository temporaryPostJpaRepository;
    private final UserInfoRepository userInfoRepository;
    private final ImageConverter imageConverter;

    @Transactional(readOnly = true)
    private void checkAuthority(SignUserInfo signUserInfo, long temporaryId){
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 임시저장글"));

        if(!temporaryPost.getUserInfo().getProfileId().equals(signUserInfo.profileId())
                && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근권한 부족");
        }
    }

    @Transactional
    public TemporaryKeyResponse issueTemporaryId(SignUserInfo signUserInfo){
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        TemporaryPost temporaryPost = new TemporaryPost(userInfo);
        temporaryPostJpaRepository.save(temporaryPost);

        return new TemporaryKeyResponse(temporaryPost.getTemporaryId());
    }

    @Transactional(readOnly = true)
    public TemporaryPostResponse getTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        checkAuthority(signUserInfo, temporaryId);

        return TemporaryPostResponse.from(temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글")));
    }

    @Transactional(readOnly = true)
    public List<TemporaryPostTitleResponse> getTemporaryPosts(SignUserInfo signUserInfo){
        return temporaryPostJpaRepository.findByUserInfo_ProfileId(signUserInfo.profileId())
                .stream().map(TemporaryPostTitleResponse::from).toList();
    }

    @Transactional
    public TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostRequest postRequest) throws IOException {
        System.out.println("service");
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        String image = null;
        if(postRequest.image() != null){
            image = imageConverter.updatePostImage(postRequest.image());
        }
        System.out.println(postRequest.title() + " " +postRequest.content());
        temporaryPost.update(postRequest.title(), postRequest.content(), image);

        return TemporaryPostResponse.from(temporaryPost);
    }

    @Transactional
    public void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        temporaryPostJpaRepository.delete(temporaryPost);
    }
}
