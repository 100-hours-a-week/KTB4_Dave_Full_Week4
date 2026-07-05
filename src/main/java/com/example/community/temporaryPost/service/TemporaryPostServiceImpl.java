package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.dto.response.request.PostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.temporaryPost.repository.TemporaryPostJpaRepository;
import com.example.community.user.repository.UserInfoJpaRepository;
import com.example.community.resolver.SignUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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
    public TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostRequest postRequest) throws IOException {
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        temporaryPost.update(postRequest.title(), postRequest.content(), updatePostImage(postRequest.image()));

        return TemporaryPostResponse.from(temporaryPost);
    }

    public void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId){
        checkAuthority(signUserInfo, temporaryId);
        TemporaryPost temporaryPost = temporaryPostJpaRepository.findByTemporaryId(temporaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 임시저장 글"));
        temporaryPostJpaRepository.delete(temporaryPost);
    }
}
