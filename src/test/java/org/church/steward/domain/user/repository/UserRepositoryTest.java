package org.church.steward.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("findByRole 테스트")
    class FindByRoleTest {

        @Test
        @DisplayName("지정된 역할을 가진 사용자 목록을 반환한다")
        void findByRole_ShouldReturnUsersWithSpecificRole() {
            // Given
            User admin1 = User.builder()
                    .id("admin1")
                    .password("hashed_password")
                    .role(UserRole.ADMIN)
                    .build();
            User admin2 = User.builder()
                    .id("admin2")
                    .password("hashed_password")
                    .role(UserRole.ADMIN)
                    .build();
            User user1 = User.builder()
                    .id("user1")
                    .password("hashed_password")
                    .role(UserRole.USER)
                    .build();

            userRepository.saveAll(List.of(admin1, admin2, user1));

            // When
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);

            // Then
            assertThat(admins).hasSize(2);
            assertThat(admins).extracting(User::getId).containsExactlyInAnyOrder("admin1", "admin2");
        }

        @Test
        @DisplayName("해당 역할을 가진 사용자가 없으면 빈 목록을 반환한다")
        void findByRole_ShouldReturnEmptyListWhenNoUserWithRoleExists() {
            // Given
            User user1 = User.builder()
                    .id("user1")
                    .password("hashed_password")
                    .role(UserRole.USER)
                    .build();
            userRepository.save(user1);

            // When
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);

            // Then
            assertThat(admins).isEmpty();
        }
    }
}
