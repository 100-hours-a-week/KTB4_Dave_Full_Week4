package com.example.community.post.service;

import com.example.community.post.cache.PopularPostSnapshot;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import com.example.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PopularPostSnapshotLoader {
    private final PopularPostRankingQueryService rankingQueryService;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public PopularPostSnapshot load() {
        List<Long> postNums = rankingQueryService.getTop10PopularPostNums();
        if (postNums.isEmpty()) {
            return PopularPostSnapshot.from(List.of());
        }

        Map<Long, PopularPostTitleResponse> summariesByPostNum =
                new HashMap<>();
        postRepository.findPopularPostTitlesByPostNumIn(postNums)
                .forEach(summary -> summariesByPostNum.put(
                        summary.postNum(),
                        summary
                ));

        List<PopularPostTitleResponse> orderedSummaries = postNums.stream()
                .map(summariesByPostNum::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return PopularPostSnapshot.from(orderedSummaries);
    }
}
