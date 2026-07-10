package com.example.community.user.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.dto.response.PostTitleResponse;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.PasswordChangeRequest;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.request.SignUpRequest;
import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.dto.response.SignUpResponse;
import com.example.community.user.dto.response.UserDeleteResponse;
import com.example.community.user.dto.response.UserInfoResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.util.ImageConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;


@Service
public class UserService{
    private final SignInfoRepository signInfoRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLikeRepository userLikeRepository;
    private final ImageConverter imageConverter;
    private final PasswordEncoder passwordEncoder;

    public UserService(SignInfoRepository signInfoRepository,
                       UserLikeRepository userLikeRepository, UserInfoRepository userInfoRepository,
                       ImageConverter imageConverter, PasswordEncoder passwordEncoder){
        this.signInfoRepository = signInfoRepository;
        this.userInfoRepository = userInfoRepository;
        this.userLikeRepository = userLikeRepository;
        this.imageConverter = imageConverter;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public SignUpResponse signUp(SignUpRequest signUpRequest) throws IOException {
        if(signInfoRepository.existsByEmail(signUpRequest.email())){
            throw new DuplicateException("중복 이메일 존재");
        }
        if(userInfoRepository.existsByNickname(signUpRequest.nickname())){
            throw new DuplicateException("중복 닉네임 존재");
        }
        if(!signUpRequest.password().equals(signUpRequest.passwordConfirm())){
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        String image = null;
        if(signUpRequest.imageFile() != null) {
            image = imageConverter.updateProfileImage(signUpRequest.imageFile());
        }
        SignInfo signInfo = new SignInfo(signUpRequest.email(), passwordEncoder.encode(signUpRequest.password()));
        signInfoRepository.save(signInfo);
        UserInfo userInfo = new UserInfo(signInfo, signUpRequest.nickname(), image);
        userInfoRepository.save(userInfo);

        return new SignUpResponse(signInfo.getUserNum());
    }

    @Transactional(readOnly = true)
    public UserInfoDTO signIn(SignInRequest signInRequest) {
        SignInfo signInfo = signInfoRepository.findByEmail(signInRequest.email())
                .orElseThrow(() -> new NotFoundException("존재하지 않는 이메일"));

        //현재는 평문이 저장되겠으나 암호화된 값을 비교해야 함.
        if(!passwordEncoder.matches(signInRequest.password(), signInfo.getPassword())){
            throw new UnAuthorizedException("로그인 실패");
        }
        if(signInfo.isDeleted()){
            throw new UnAuthorizedException("탈퇴한 유저");
        }

        return UserInfoDTO.from(userInfoRepository.findBySignInfo_UserNum(signInfo.getUserNum()).getFirst());
    }

    @Transactional(readOnly = true)
    public boolean isExistEmail(String email) {
        return signInfoRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isExistNickname(String nickname) {
        return userInfoRepository.existsByNickname(nickname);
    }

    @Transactional
    public UserInfoResponse updateUserInfo(SignUserInfo signUserInfo, UserInfoRequest userInfoRequest) throws IOException {
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        if(isExistNickname(userInfoRequest.nickname())){
            throw new DuplicateException("중복 닉네임 존재");
        }
        String profileImage = null;
        if(userInfoRequest.imageFile() != null) {
            profileImage = imageConverter.updateProfileImage(userInfoRequest.imageFile());
        }
        userInfo.update(userInfoRequest.nickname(), profileImage);
        return UserInfoResponse.from(userInfo);
    }

    @Transactional
    public void changePassword(SignUserInfo signUserInfo, PasswordChangeRequest passwordChangeRequest) {
        SignInfo signInfo = signInfoRepository.findByUserNum(signUserInfo.userNum())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));

        if(!passwordEncoder.matches(passwordChangeRequest.password(), signInfo.getPassword())){
            throw new BadRequestException("비밀번호가 틀렸습니다.");
        }
        if(!passwordChangeRequest.nextPassword().equals(passwordChangeRequest.passwordConfirm())){
            throw new BadRequestException("비밀번호 확인 불일치");
        }

        signInfo.changePassword(passwordChangeRequest.nextPassword());
    }

    @Transactional(readOnly = true)
    public PostPageResponse getLikePosts(long profileId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserLikePost> userLikePosts = userLikeRepository.findByUserInfo_ProfileId(profileId, pageable);
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

    @Transactional
    public UserDeleteResponse deleteUser(SignUserInfo signUserInfo) {
        SignInfo signInfo = signInfoRepository.findByUserNum(signUserInfo.userNum())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        UserInfo userInfo = userInfoRepository.findByProfileId(signUserInfo.profileId())
                .orElseThrow(()-> new NotFoundException("존재하지 않는 유저"));
        signInfo.delete();
        userInfo.delete();

        return new UserDeleteResponse(signUserInfo.userNum(), signInfo.isDeleted());
    }
}
