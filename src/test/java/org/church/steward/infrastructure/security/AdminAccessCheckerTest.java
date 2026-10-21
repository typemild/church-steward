package org.church.steward.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.church.steward.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAccessCheckerTest {

    @Mock
    private UserSessionHolder userSessionHolder;

    @InjectMocks
    private AdminAccessChecker adminAccessChecker;

    @Nested
    @DisplayName("checkAdmin 테스트")
    class CheckAdminTest {

        @Test
        @DisplayName("로그인한 사용자가 관리자이면 예외 없이 통과한다")
        void checkAdmin_Admin_Passes() {
            // Given
            when(userSessionHolder.getLoggedInUser()).thenReturn(new LoggedInUser("admin", UserRole.ADMIN));

            // When & Then
            assertThatCode(() -> adminAccessChecker.checkAdmin()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("로그인한 사용자가 관리자가 아니면 AccessDeniedException을 던진다")
        void checkAdmin_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            when(userSessionHolder.getLoggedInUser()).thenReturn(new LoggedInUser("user", UserRole.USER));

            // When & Then
            assertThatThrownBy(() -> adminAccessChecker.checkAdmin())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");
        }

        @Test
        @DisplayName("로그인한 사용자가 없으면 AccessDeniedException을 던진다")
        void checkAdmin_NoUser_ThrowsAccessDeniedException() {
            // Given
            when(userSessionHolder.getLoggedInUser()).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> adminAccessChecker.checkAdmin())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");
        }
    }
}
