package org.church.steward.domain.user.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserRoleTransformerTest {

    private final UserRoleTransformer transformer = new UserRoleTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("UserRole.ADMIN일 때 '관리자'를 반환한다")
        void transform_Admin_ReturnsLabel() {
            // Given
            UserRole role = UserRole.ADMIN;

            // When
            Object result = transformer.transform(role);

            // Then
            assertThat(result).isEqualTo("관리자");
        }

        @Test
        @DisplayName("UserRole.USER일 때 '사용자'를 반환한다")
        void transform_User_ReturnsLabel() {
            // Given
            UserRole role = UserRole.USER;

            // When
            Object result = transformer.transform(role);

            // Then
            assertThat(result).isEqualTo("사용자");
        }

        @Test
        @DisplayName("UserRole 타입이 아닌 값이 들어오면 IllegalArgumentException을 던진다")
        void transform_InvalidType_ThrowsException() {
            // Given
            String invalidValue = "NOT_A_ROLE";

            // When & Then
            assertThatThrownBy(() -> transformer.transform(invalidValue))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null 값이 들어오면 IllegalArgumentException을 던진다")
        void transform_NullValue_ThrowsException() {
            // Given
            Object nullValue = null;

            // When & Then
            assertThatThrownBy(() -> transformer.transform(nullValue))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
