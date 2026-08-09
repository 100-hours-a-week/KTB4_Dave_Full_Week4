package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.cache.PopularPostDetailStore;
import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostDetailReadService {
    private final PopularPostSnapshotService snapshotService;
    private final PopularPostDetailStore detailStore;
    private final PostRepository postRepository;
    private final PostStateRepository postStateRepository;

    @Transactional(readOnly = true)
    public PostDetailData read(long postNum) {
        boolean popular = snapshotService.isPopular(postNum);
        PostBodyData body = popular
                ? detailStore.getBody(postNum, this::loadBody)
                : loadBody(postNum);
        PostStateData state = popular
                ? detailStore.getState(postNum, this::loadState)
                : loadState(postNum);
        return new PostDetailData(body, state);
    }

    private PostBodyData loadBody(long postNum) {
        return postRepository.findPostBodyDataByPostNum(postNum)
                .orElseThrow(this::postNotFound);
    }

    private PostStateData loadState(long postNum) {
        return postStateRepository.findDataByPostNum(postNum)
                .orElseThrow(this::postNotFound);
    }

    private NotFoundException postNotFound() {
        return new NotFoundException("존재하지 않는 게시글");
    }
}
