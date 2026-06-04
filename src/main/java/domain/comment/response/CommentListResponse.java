package domain.comment.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CommentListResponse {
    CommentResponse comment;
    List<CommentListResponse> childComments;
}
