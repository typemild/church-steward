package org.church.steward.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserSessionHolderImplTest {

    private UserSessionHolderImpl sessionHolder;
    private LoggedInUser testUser;

    @BeforeEach
    void setUp() {
        sessionHolder = new UserSessionHolderImpl();
        testUser = new LoggedInUser("test_user", UserRole.USER);
    }

    @Nested
    @DisplayName("getLoggedInUser 테스트")
    class GetLoggedInUserTest {

        @Test
        @DisplayName("초기 상태의 로그인 사용자는 null이어야 한다.")
        void getLoggedInUser_ShouldReturnNull_WhenNoUserIsSet() {
            // Then
            assertThat(sessionHolder.getLoggedInUser()).isNull();
        }
    }

    @Nested
    @DisplayName("setLoggedInUser 테스트")
    class SetLoggedInUserTest {

        @Test
        @DisplayName("사용자를 설정하면 저장된 사용자를 올바르게 가져올 수 있어야 한다.")
        void setLoggedInUser_ShouldStoreAndRetrieveUser() {
            // When
            sessionHolder.setLoggedInUser(testUser);

            // Then
            assertThat(sessionHolder.getLoggedInUser()).isEqualTo(testUser);
            assertThat(sessionHolder.getLoggedInUser().id()).isEqualTo("test_user");
        }
    }

    @Nested
    @DisplayName("clear 테스트")
    class ClearTest {

        @Test
        @DisplayName("clear() 호출 시 저장된 사용자 정보가 삭제되어야 한다.")
        void clear_ShouldRemoveLoggedInUser() {
            // Given
            sessionHolder.setLoggedInUser(testUser);
            assertThat(sessionHolder.getLoggedInUser()).isNotNull();

            // When
            sessionHolder.clear();

            // Then
            assertThat(sessionHolder.getLoggedInUser()).isNull();
        }
    }
}
