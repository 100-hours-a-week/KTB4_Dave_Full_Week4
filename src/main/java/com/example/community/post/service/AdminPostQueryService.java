package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.AdminPostPageResponse;
import com.example.community.post.dto.response.AdminPostResponse;
import com.example.community.post.dto.response.PostEditPageResponse;
import com.example.community.post.dto.response.PostEditResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.util.ImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPostQueryService {
    private final PostRepository postRepository;
    private final PostEditRepository postEditRepository;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional(readOnly = true)
    public AdminPostPageResponse getPostsByPage(
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                PostSortPolicy.forPosts(sort)
        );
        return AdminPostPageResponse.from(
                postRepository.findPostByPage(pageable),
                imageUrlBuilder
        );
    }

    @Transactional(readOnly = true)
    public AdminPostResponse getPost(long postNum) {
        return AdminPostResponse.from(findPost(postNum), imageUrlBuilder);
    }

    @Transactional(readOnly = true)
    public PostEditPageResponse getPostEditsByPage(
            long postNum,
            int page,
            int size
    ) {
        Page<PostEditRecord> postEdits = postEditRepository
                .findByPost_PostNumOrderByEditIdDesc(
                        postNum,
                        PageRequest.of(page, size)
                );
        return PostEditPageResponse.from(postEdits);
    }

    @Transactional(readOnly = true)
    public PostEditResponse getPostEdit(long editId) {
        PostEditRecord edit = postEditRepository.findById(editId)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 수정 이력")
                );
        return PostEditResponse.from(edit);
    }

    private Post findPost(long postNum) {
        return postRepository.findByPostNum(postNum)
                .orElseThrow(() ->
                        new NotFoundException("존재하지 않는 게시글")
                );
    }
}
