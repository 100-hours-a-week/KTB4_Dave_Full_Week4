package com.example.community.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignInfoTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("이메일이 null이거나 공백이면 회원 정보를 생성할 수 없다")
    void constructorRejectsBlankEmail(String email) {
        assertThatThrownBy(() ->
                new SignInfo(email, "encoded-password")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email은 필수입니다.");
    }

    @Test
    @DisplayName("이메일이 60자를 초과하면 회원 정보를 생성할 수 없다")
    void constructorRejectsEmailLongerThanSixtyCharacters() {
        assertThatThrownBy(() ->
                new SignInfo("a".repeat(61), "encoded-password")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일이 너무 깁니다.");
    }

    @Test
    @DisplayName("이메일이 정확히 60자면 회원 정보를 생성할 수 있다")
    void constructorAcceptsSixtyCharacterEmail() {
        SignInfo signInfo = new SignInfo(
                "a".repeat(60),
                "encoded-password"
        );

        assertThat(signInfo.getEmail()).hasSize(60);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("비밀번호가 공백이면 회원 정보를 생성할 수 없다")
    void constructorRejectsBlankPassword(String password) {
        assertThatThrownBy(() ->
                new SignInfo("user@example.com", password)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 8자 이상 20자 이하여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 값이 공백이면 기존 비밀번호를 유지한다")
    void changePasswordRejectsBlankValueAndKeepsExistingPassword() {
        SignInfo signInfo = signInfo();

        assertThatThrownBy(() -> signInfo.changePassword(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호는 8자 이상 20자 이하여야 합니다.");
        assertThat(signInfo.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("유효한 비밀번호 변경 값은 반영한다")
    void changePasswordAcceptsValidValue() {
        SignInfo signInfo = signInfo();

        signInfo.changePassword("next-encoded-password");

        assertThat(signInfo.getPassword())
                .isEqualTo("next-encoded-password");
    }

    @Test
    @DisplayName("탈퇴 처리하면 회원을 탈퇴 상태로 변경한다")
    void deleteChangesDeletedState() {
        SignInfo signInfo = signInfo();
        assertThat(signInfo.isDeleted()).isFalse();

        signInfo.delete();

        assertThat(signInfo.isDeleted()).isTrue();
        assertThat(signInfo.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 식별자의 회원 정보는 동등하고 같은 해시 코드를 가진다")
    void equalityUsesPersistedIdentifier() {
        SignInfo first = new SignInfo(
                1L,
                "first@example.com",
                "first-password",
                null,
                Instant.parse("2026-08-01T00:00:00Z")
        );
        SignInfo second = new SignInfo(
                1L,
                "second@example.com",
                "second-password",
                null,
                Instant.parse("2026-08-02T00:00:00Z")
        );

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("아직 저장되지 않은 서로 다른 회원 정보는 동등하지 않다")
    void transientEntitiesAreNotEqual() {
        assertThat(new SignInfo("user@example.com", "password"))
                .isNotEqualTo(new SignInfo("user@example.com", "password"));
    }

    private SignInfo signInfo() {
        return new SignInfo("user@example.com", "encoded-password");
    }
}
