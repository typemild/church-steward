package org.church.steward.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    @Nested
    @DisplayName("isPasswordChanged 테스트")
    class IsPasswordChangedTest {

        @Test
        @DisplayName("isPasswordChanged_비밀번호를 바꾸지 않았으면 false를 반환해야 한다.")
        void isPasswordChanged_NeverChanged_ReturnsFalse() {
            // Given
            User user = User.builder().id("user1").password("encoded_password").build();

            // When
            boolean changed = user.isPasswordChanged();

            // Then
            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("isPasswordChanged_비밀번호를 바꾸면 true를 반환해야 한다.")
        void isPasswordChanged_AfterSetPassword_ReturnsTrue() {
            // Given
            User user = User.builder().id("user1").password("encoded_password").build();
            user.setPassword("encoded_new_password");

            // When
            boolean changed = user.isPasswordChanged();

            // Then
            assertThat(changed).isTrue();
        }
    }
}
