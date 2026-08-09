package com.example.community.post.service;

import com.example.community.post.repository.PostPopularityStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularPostRankingQueryService {
    private static final int POPULAR_POST_LIMIT = 10;

    private final PostPopularityStatRepository postPopularityStatRepository;
    private final Clock clock;
    private final PopularityWindowPolicy windowPolicy;

    @Transactional(readOnly = true)
    public List<Long> getTop10PopularPostNums() {
        return postPopularityStatRepository.findPopularPostNums(
                windowPolicy.candidateSince(clock.instant()),
                PageRequest.of(0, POPULAR_POST_LIMIT)
        );
    }
}
