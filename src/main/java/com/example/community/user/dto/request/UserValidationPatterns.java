package com.example.community.user.dto.request;

final class UserValidationPatterns {
    static final String PASSWORD =
            "^(?=\\S*$)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[~!@#$%^&*]).*$";

    private UserValidationPatterns() {
    }
}
