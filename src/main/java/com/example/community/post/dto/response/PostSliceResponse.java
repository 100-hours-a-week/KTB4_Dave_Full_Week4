package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import org.springframework.data.domain.Slice;

import java.util.List;

public record PostSliceResponse(
        List<PostTitleResponse> postTitleResponses,
        int page,
        int pageSize,
        int postCount,
        boolean hasNext
) {
    public static PostSliceResponse from(Slice<Post> postSlice){
        return new PostSliceResponse(
                postSlice.getContent().stream().map(PostTitleResponse::from).toList(),
                postSlice.getNumber(),
                postSlice.getSize(),
                postSlice.getNumberOfElements(),
                postSlice.hasNext()
        );
    }
}
