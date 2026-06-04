package domain.user.response;

import domain.user.User;

public record UserDeleteResponse(
        long userNum,
        boolean isDeleted
) {
    public static UserDeleteResponse from(User user){
        return new UserDeleteResponse(user.getUserNum(), user.isDeleted());
    }
}
