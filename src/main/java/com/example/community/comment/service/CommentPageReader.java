package com.example.community.comment.service;

import com.example.community.comment.cache.PopularCommentFirstPageIndex;
import com.example.community.comment.cache.PopularCommentStore;
import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.post.service.PopularPostSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentPageReader {
    private static final Sort REGISTRATION_ORDER =
            Sort.by(Sort.Direction.ASC, "commentNum");

    private final CommentRepository commentRepository;
    private final PopularPostSnapshotService snapshotService;
    private final PopularCommentStore commentStore;

    @Transactional(readOnly = true)
    public CommentPageResponse read(long postNum, int page, int size) {
        if (!isCacheTarget(postNum, page, size)) {
            return readWithoutCache(postNum, page, size);
        }
        return readPopularFirstPage(postNum, true);
    }

    private boolean isCacheTarget(long postNum, int page, int size) {
        return page == PopularCommentFirstPageIndex.PAGE
                && size == PopularCommentFirstPageIndex.PAGE_SIZE
                && snapshotService.isPopular(postNum);
    }

    private CommentPageResponse readWithoutCache(
            long postNum,
            int page,
            int size
    ) {
        Page<Comment> comments = commentRepository.findByPost_postNum(
                postNum,
                pageRequest(page, size)
        );
        return CommentPageResponse.from(comments);
    }

    private CommentPageResponse readPopularFirstPage(
            long postNum,
            boolean allowIndexRefresh
    ) {
        PopularCommentFirstPageIndex index = commentStore.getIndex(
                postNum,
                this::loadFirstPageIndex
        );
        Map<Long, CommentResponse> commentsByNum =
                loadIndexedComments(index);

        if (commentsByNum.size() != index.commentNums().size()) {
            commentStore.invalidateIndex(postNum);
            if (allowIndexRefresh) {
                return readPopularFirstPage(postNum, false);
            }
        }
        return toResponse(index, commentsByNum);
    }

    private PopularCommentFirstPageIndex loadFirstPageIndex(long postNum) {
        Page<CommentResponse> page = commentRepository
                .findFirstPageResponsesByPostNum(
                        postNum,
                        pageRequest(
                                PopularCommentFirstPageIndex.PAGE,
                                PopularCommentFirstPageIndex.PAGE_SIZE
                        )
                );
        List<CommentResponse> comments = page.getContent();
        commentStore.putComments(comments);
        return new PopularCommentFirstPageIndex(
                comments.stream()
                        .map(CommentResponse::commentNum)
                        .toList(),
                page.getTotalElements()
        );
    }

    private Map<Long, CommentResponse> loadIndexedComments(
            PopularCommentFirstPageIndex index
    ) {
        Map<Long, CommentResponse> commentsByNum = new HashMap<>();
        List<Long> missingCommentNums = new ArrayList<>();

        for (Long commentNum : index.commentNums()) {
            CommentResponse cached =
                    commentStore.getCommentIfPresent(commentNum);
            if (cached == null) {
                missingCommentNums.add(commentNum);
            } else {
                commentsByNum.put(commentNum, cached);
            }
        }

        if (!missingCommentNums.isEmpty()) {
            List<CommentResponse> loaded = commentRepository
                    .findResponsesByCommentNumIn(missingCommentNums);
            commentStore.putComments(loaded);
            loaded.forEach(comment -> commentsByNum.put(
                    comment.commentNum(),
                    comment
            ));
        }
        return commentsByNum;
    }

    private CommentPageResponse toResponse(
            PopularCommentFirstPageIndex index,
            Map<Long, CommentResponse> commentsByNum
    ) {
        List<CommentResponse> orderedComments = index.commentNums()
                .stream()
                .map(commentsByNum::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new CommentPageResponse(
                orderedComments,
                PopularCommentFirstPageIndex.PAGE,
                PopularCommentFirstPageIndex.PAGE_SIZE,
                orderedComments.size(),
                index.totalCount(),
                index.totalPage()
        );
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, REGISTRATION_ORDER);
    }
}
