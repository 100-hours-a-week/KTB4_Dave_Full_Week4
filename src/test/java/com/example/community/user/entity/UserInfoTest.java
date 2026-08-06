package com.example.community.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoTest {

    @Test
    @DisplayName("탈퇴한 사용자는 닉네임과 프로필 이미지를 마스킹하고 로그인 정보도 탈퇴 처리한다")
    void deleteMasksProfileAndDeletesSignInfo() {
        SignInfo signInfo = new SignInfo(
                "user@example.com",
                "encoded-password"
        );
        UserInfo userInfo = new UserInfo(
                signInfo,
                "nickname",
                "profiles/profile.png"
        );
        assertThat(userInfo.isDeleted()).isFalse();
        assertThat(userInfo.getNickname()).isEqualTo("nickname");
        assertThat(userInfo.getProfileImage())
                .isEqualTo("profiles/profile.png");
        assertThat(signInfo.isDeleted()).isFalse();

        userInfo.delete();

        assertThat(userInfo.isDeleted()).isTrue();
        assertThat(userInfo.getNickname()).isEqualTo("알 수 없음");
        assertThat(userInfo.getProfileImage()).isNull();
        assertThat(signInfo.isDeleted()).isTrue();
    }
}
