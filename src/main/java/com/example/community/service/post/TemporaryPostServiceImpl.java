package com.example.community.service.post;

import com.example.community.domain.exception.ForbiddenException;
import com.example.community.domain.exception.NotFoundException;
import com.example.community.domain.post.TemporaryPost;
import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.TemporaryKeyResponse;
import com.example.community.domain.post.response.TemporaryPostResponse;
import com.example.community.domain.post.response.TemporaryPostTitleResponse;
import com.example.community.domain.user.UserInfo;
import com.example.community.domain.user.UserRole;
import com.example.community.repository.post.TemporaryPostJpaRepository;
import com.example.community.repository.user.UserInfoJpaRepository;
import com.example.community.resolver.SignUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemporaryPostServiceImpl implements TemporaryPostService{
    private final TemporaryPostJpaRepository temporaryPostJpaRepository;
    private final UserInfoJpaRepository userInfoJpaRepository;

    private void checkAuthority(SignUserInfo signUserInfo, long temporaryId){
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 임시저장글"));

        if(!temporaryPost.getUserInfo().getProfileId().equals(signUserInfo.profileId())
                && signUserInfo.userRole() != UserRole.ADMIN){
            throw new ForbiddenException("접근권한 부족");
        }
    }

    public TemporaryKeyResponse issueTemporaryId(SignUserInfo signUserInfo){
        UserInfo userInfo = userInfoJpaRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        TemporaryPost temporaryPost = new TemporaryPost(userInfo);
        temporaryPostJpaRepository.save(temporaryPost);

        return new TemporaryKeyResponse(temporaryPost.getTemporaryId());
    }

    public TemporaryPostResponse getTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        checkAuthority(signUserInfo, temporaryId);

        return TemporaryPostResponse.from(temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글")));
    }

    public List<TemporaryPostTitleResponse> getTemporaryPosts(SignUserInfo signUserInfo){
        return temporaryPostJpaRepository.findByUserInfo_ProfileId(signUserInfo.profileId())
                .stream().map(TemporaryPostTitleResponse::from).toList();
    }

    public TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostRequest postRequest){
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        temporaryPost.update(postRequest.title(), postRequest.content(), postRequest.image());

        return TemporaryPostResponse.from(temporaryPost);
    }

    public void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        temporaryPostJpaRepository.delete(temporaryPost);
    }
}
