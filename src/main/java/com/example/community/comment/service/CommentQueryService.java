package com.example.community.comment.service;

import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentQueryService {
    private static final Sort REGISTRATION_ORDER =
            Sort.by(Sort.Direction.ASC, "commentNum");

    private final CommentRepository commentRepository;
    private final CommentPageReader commentPageReader;

    public CommentPageResponse getPostCommentPage(
            long postNum,
            int page,
            int size
    ) {
        return commentPageReader.read(postNum, page, size);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getChildCommentPage(
            long commentNum,
            int page,
            int size
    ) {
        Page<Comment> comments = commentRepository.findByParentNum(
                commentNum,
                pageRequest(page, size)
        );
        return CommentPageResponse.from(comments);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, REGISTRATION_ORDER);
    }
}
