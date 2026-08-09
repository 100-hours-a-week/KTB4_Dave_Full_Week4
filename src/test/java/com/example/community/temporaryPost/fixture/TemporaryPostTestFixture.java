package com.example.community.temporaryPost.fixture;

import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

public final class TemporaryPostTestFixture {
    public static final Instant WRITE_AT =
            Instant.parse("2026-08-05T00:00:00Z");

    private TemporaryPostTestFixture() {
    }

    public static UserInfo user(long profileId, String nickname) {
        UserInfo userInfo = new UserInfo(
                new SignInfo(nickname + "@example.com", "encoded-password"),
                nickname,
                null
        );
        userInfo.setProfileId(profileId);
        return userInfo;
    }

    public static TemporaryPost temporaryPost(
            long temporaryId,
            UserInfo owner,
            String title,
            String content,
            String image
    ) {
        TemporaryPost temporaryPost = new TemporaryPost(owner);
        temporaryPost.update(title, content, image);
        ReflectionTestUtils.setField(
                temporaryPost,
                "temporaryId",
                temporaryId
        );
        ReflectionTestUtils.setField(temporaryPost, "writeAt", WRITE_AT);
        return temporaryPost;
    }
}
