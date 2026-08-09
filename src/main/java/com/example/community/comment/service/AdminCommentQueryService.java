package com.example.community.comment.service;

import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.comment.repository.CommentEditRepository;
import com.example.community.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCommentQueryService {
    private static final Sort REGISTRATION_ORDER =
            Sort.by(Sort.Direction.ASC, "commentNum");

    private final CommentRepository commentRepository;
    private final CommentEditRepository commentEditRepository;

    @Transactional(readOnly = true)
    public CommentPageResponse getPostCommentPage(
            long postNum,
            int page,
            int size
    ) {
        Page<Comment> comments = commentRepository.findByPost_postNum(
                postNum,
                commentPageRequest(page, size)
        );
        return CommentPageResponse.adminFrom(comments);
    }

    @Transactional(readOnly = true)
    public CommentPageResponse getChildCommentPage(
            long commentNum,
            int page,
            int size
    ) {
        Page<Comment> comments = commentRepository.findByParentNum(
                commentNum,
                commentPageRequest(page, size)
        );
        return CommentPageResponse.adminFrom(comments);
    }

    @Transactional(readOnly = true)
    public CommentEditPageResponse getCommentEditsByPage(
            long commentNum,
            int page,
            int size
    ) {
        Page<CommentEditRecord> comments = commentEditRepository
                .findByComment_CommentNumOrderByEditIdDesc(
                        commentNum,
                        PageRequest.of(page, size)
                );
        return CommentEditPageResponse.from(comments);
    }

    private PageRequest commentPageRequest(int page, int size) {
        return PageRequest.of(page, size, REGISTRATION_ORDER);
    }
}
