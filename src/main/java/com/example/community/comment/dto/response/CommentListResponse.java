package com.example.community.comment.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class CommentListResponse {
    CommentResponse comment;
    List<CommentListResponse> childComments;

    public static CommentListResponse of(CommentResponse comment){
        return new CommentListResponse(comment, new ArrayList<>());
    }
    public void addChild(List<CommentListResponse> commentListResponse){
        childComments.addAll(commentListResponse);
    }
}
