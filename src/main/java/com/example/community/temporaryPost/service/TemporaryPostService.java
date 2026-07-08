package com.example.community.temporaryPost.service;

import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.dto.response.request.PostRequest;

import java.io.IOException;
import java.util.List;

public interface TemporaryPostService {
    TemporaryKeyResponse issueTemporaryId(SignUserInfo signUserInfo);
    TemporaryPostResponse getTemporaryPost(SignUserInfo signUserInfo, long temporaryId);
    List<TemporaryPostTitleResponse> getTemporaryPosts(SignUserInfo signUserInfo);
    TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostRequest postRequest) throws IOException;
    void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId);
}
