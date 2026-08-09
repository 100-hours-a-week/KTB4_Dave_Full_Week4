package com.example.community.post.fixture;

import com.example.community.post.cache.PopularPostSnapshot;
import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.service.PostDetailData;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;

public final class PostTestFixture {
    public static final Instant WRITE_AT =
            Instant.parse("2026-08-01T00:00:00Z");

    private PostTestFixture() {
    }

    public static UserInfo user(long profileId, String nickname) {
        return user(profileId, nickname, null);
    }

    public static UserInfo user(
            long profileId,
            String nickname,
            String profileImage
    ) {
        UserInfo userInfo = new UserInfo(
                new SignInfo(nickname + "@example.com", "password"),
                nickname,
                profileImage
        );
        userInfo.setProfileId(profileId);
        return userInfo;
    }

    public static Post post(long postNum, UserInfo author) {
        Post post = new Post(
                author,
                "title-" + postNum,
                "content-" + postNum,
                null
        );
        ReflectionTestUtils.setField(post, "postNum", postNum);
        ReflectionTestUtils.setField(post, "writeAt", WRITE_AT);
        return post;
    }

    public static PostDetailData detail(int reportCount) {
        PostBodyData body = new PostBodyData(
                10L,
                "author",
                "profiles/author.png",
                null,
                "title-10",
                "content-10",
                "posts/detail.png",
                null,
                WRITE_AT
        );
        PostStateData state = new PostStateData(0, 0, reportCount, 0);
        return new PostDetailData(body, state);
    }

    public static PopularPostSnapshot snapshot(long... postNums) {
        return PopularPostSnapshot.from(Arrays.stream(postNums)
                .mapToObj(postNum -> new PopularPostTitleResponse(
                        postNum,
                        "author",
                        null,
                        null,
                        "title-" + postNum,
                        WRITE_AT
                ))
                .toList());
    }

    public static PostEditRecord editRecord(long editId, Post post) {
        PostEditRecord editRecord = new PostEditRecord(post);
        ReflectionTestUtils.setField(editRecord, "editId", editId);
        ReflectionTestUtils.setField(editRecord, "title", "old-title");
        ReflectionTestUtils.setField(editRecord, "content", "old-content");
        ReflectionTestUtils.setField(editRecord, "writeAt", WRITE_AT);
        return editRecord;
    }

    public static void blind(Post post) {
        for (int reportCount = 0; reportCount < 6; reportCount++) {
            post.report();
        }
    }
}
