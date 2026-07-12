package com.example.community.comment.dto.response;

import com.example.community.comment.entity.Comment;
import org.springframework.data.domain.Page;

import java.util.List;

public record CommentPageResponse (
        List<CommentResponse> commentResponses,
        int page,
        int pageSize,
        int commentCount,
        long totalCount,
        int totalPage
) {
    public static CommentPageResponse from(Page<Comment> commentPage){
        return new CommentPageResponse(
                commentPage.getContent().stream().map(CommentResponse::from).toList(),
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getNumberOfElements(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages()
        );
    }

    public static CommentPageResponse adminFrom(Page<Comment> commentPage){
        return new CommentPageResponse(
                commentPage.getContent().stream().map(CommentResponse::adminFrom).toList(),
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getNumberOfElements(),
                commentPage.getTotalElements(),
                commentPage.getTotalPages()
        );
    }
}
