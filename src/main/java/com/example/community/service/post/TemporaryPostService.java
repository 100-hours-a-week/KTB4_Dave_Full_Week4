package com.example.community.service.post;

import com.example.community.domain.post.request.PostRequest;
import com.example.community.domain.post.response.TemporaryKeyResponse;
import com.example.community.domain.post.response.TemporaryPostResponse;
import com.example.community.domain.post.response.TemporaryPostTitleResponse;
import com.example.community.resolver.SignUserInfo;

import java.io.IOException;
import java.util.List;

public interface TemporaryPostService {
    TemporaryKeyResponse issueTemporaryId(SignUserInfo signUserInfo);
    TemporaryPostResponse getTemporaryPost(SignUserInfo signUserInfo, long temporaryId);
    List<TemporaryPostTitleResponse> getTemporaryPosts(SignUserInfo signUserInfo);
    TemporaryPostResponse updateTemporaryPost(SignUserInfo signUserInfo, long temporaryId, PostRequest postRequest) throws IOException;
    void deleteTemporaryPost(SignUserInfo signUserInfo, long temporaryId);
}
