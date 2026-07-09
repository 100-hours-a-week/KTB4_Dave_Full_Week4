package com.example.community.comment.dto.response;

import com.example.community.comment.entity.CommentEditRecord;
import org.springframework.data.domain.Page;

import java.util.List;

public record CommentEditPageResponse(
        List<CommentEditResponse> commentResponses,
      int page,
      int pageSize,
      int postCount,
      long totalCount,
      int totalPage
) {
    public static CommentEditPageResponse from(Page<CommentEditRecord> commentEditRecords){
        return new CommentEditPageResponse(
                commentEditRecords.getContent().stream().map(CommentEditResponse::from).toList(),
                commentEditRecords.getNumber(),
                commentEditRecords.getSize(),
                commentEditRecords.getNumberOfElements(),
                commentEditRecords.getTotalElements(),
                commentEditRecords.getTotalPages()
        );
    }
}