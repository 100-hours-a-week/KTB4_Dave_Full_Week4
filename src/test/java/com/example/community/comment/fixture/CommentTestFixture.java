package com.example.community.comment.fixture;

import com.example.community.comment.entity.Comment;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

public final class CommentTestFixture {
    private CommentTestFixture() {
    }

    public static UserInfo user(long profileId, String nickname) {
        UserInfo userInfo = new UserInfo(
                new SignInfo(nickname + "@example.com", "password"),
                nickname,
                null
        );
        userInfo.setProfileId(profileId);
        return userInfo;
    }

    public static Post post(long postNum, UserInfo author) {
        Post post = new Post(author, "title", "content", null);
        ReflectionTestUtils.setField(post, "postNum", postNum);
        return post;
    }

    public static Comment comment(
            long commentNum,
            Post post,
            UserInfo author,
            String content
    ) {
        Comment comment = new Comment(post, author, content);
        ReflectionTestUtils.setField(comment, "commentNum", commentNum);
        return comment;
    }
}
