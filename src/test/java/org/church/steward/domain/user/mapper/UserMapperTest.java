package org.church.steward.domain.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Nested
    @DisplayName("toDto 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("User 엔티티가 존재할 때 UserDto로 변환한다")
        void toDto_UserExists_Success() {
            // Given
            User user = User.builder()
                    .id("testUser")
                    .role(UserRole.ADMIN)
                    .remarks("테스트 비고")
                    .build();

            // When
            UserDto result = userMapper.toDto(user);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("testUser");
            assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(result.getRemarks()).isEqualTo("테스트 비고");
        }

        @Test
        @DisplayName("User 엔티티가 null일 때 null을 반환한다")
        void toDto_UserIsNull_ReturnsNull() {
            // Given
            User user = null;

            // When
            UserDto result = userMapper.toDto(user);

            // Then
            assertThat(result).isNull();
        }
    }
}
