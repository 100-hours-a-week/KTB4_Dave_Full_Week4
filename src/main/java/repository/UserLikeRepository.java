package repository;

import domain.user.UserLikePost;

import java.util.List;
import java.util.Optional;

public interface UserLikeRepository {
    List<UserLikePost> getUserLikePosts(long userNum);
    Optional<UserLikePost> addUserLikePost(UserLikePost userLikePost);
    void deleteUserLikePost(UserLikePost userLikePost);
}
