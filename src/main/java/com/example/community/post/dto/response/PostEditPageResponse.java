package com.example.community.post.dto.response;

import com.example.community.post.entity.PostEditRecord;
import org.springframework.data.domain.Page;

import java.util.List;

public record PostEditPageResponse(
        List<PostEditTitleResponse> postEditTitleResponses,
        int page,
        int pageSize,
        int postCount,
        long totalCount,
        int totalPage
) {
    public static PostEditPageResponse from(Page<PostEditRecord> postPage){
        return new PostEditPageResponse(
                postPage.getContent().stream().map(PostEditTitleResponse::from).toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getNumberOfElements(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
    );
}
}
