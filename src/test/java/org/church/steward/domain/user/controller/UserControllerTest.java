package org.church.steward.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.service.UserService;
import org.church.steward.infrastructure.security.AccessDeniedException;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private UserController userController;

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("createUser 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("관리자 권한인 경우 사용자 생성에 성공한다")
        void createUser_Admin_Success() {
            // Given

            // When
            userController.createUser("new_user", "password123", "remarks");

            // Then
            verify(userService).createUser("new_user", "password123", "remarks");
        }

        @Test
        @DisplayName("관리자 권한이 아닌 경우 사용자 생성에 실패한다")
        void createUser_NotAdmin_Fail() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> userController.createUser("new_user", "password123", "remarks"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("updateUser 테스트")
    class UpdateUserTest {

        @Test
        @DisplayName("관리자 권한인 경우 사용자 정보 수정에 성공한다")
        void updateUser_Admin_Success() {
            // Given

            // When
            userController.updateUser("user1", "newPassword", "newRemarks");

            // Then
            verify(userService).updateUser("user1", "newPassword", "newRemarks");
        }

        @Test
        @DisplayName("관리자 권한이 아닌 경우 사용자 정보 수정에 실패한다")
        void updateUser_NotAdmin_Fail() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> userController.updateUser("user1", "newPassword", "newRemarks"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("deleteUser 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("관리자 권한인 경우 사용자 삭제에 성공한다")
        void deleteUser_Admin_Success() {
            // Given

            // When
            userController.deleteUser("user1");

            // Then
            verify(userService).deleteUser("user1");
        }

        @Test
        @DisplayName("관리자 권한이 아닌 경우 사용자 삭제에 실패한다")
        void deleteUser_NotAdmin_Fail() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> userController.deleteUser("user1"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("updateUserPassword 테스트")
    class UpdateUserPasswordTest {

        @Test
        @DisplayName("사용자 비밀번호 변경을 호출한다")
        void updateUserPassword_Success() {
            // Given
            String userId = "testUser";
            String oldPassword = "oldPassword123";
            String newPassword = "newPassword456";

            // When
            userController.updateUserPassword(userId, oldPassword, newPassword);

            // Then
            verify(userService).updateUserPassword(userId, oldPassword, newPassword);
        }

        @Test
        @DisplayName("자신의 비밀번호 변경은 관리자 권한을 확인하지 않는다")
        void updateUserPassword_AnyUser_DoesNotCheckAdmin() {
            // Given

            // When
            userController.updateUserPassword("testUser", "oldPassword123", "newPassword456");

            // Then
            verifyNoInteractions(adminAccessChecker);
        }
    }

    @Nested
    @DisplayName("listUsersByRole 테스트")
    class ListUsersByRoleTest {

        @Test
        @DisplayName("관리자 권한인 경우 역할별 사용자 목록 조회에 성공한다")
        void listUsersByRole_Admin_Success() {
            // Given
            List<UserDto> expectedDtos = List.of(
                    UserDto.builder().id("u1").role(UserRole.USER).remarks("remark1").build(),
                    UserDto.builder().id("u2").role(UserRole.USER).remarks("remark2").build());
            when(userService.listUsersByRole(UserRole.USER)).thenReturn(expectedDtos);

            // When
            List<UserDto> result = userController.listUsersByRole(UserRole.USER);

            // Then
            assertThat(result).isEqualTo(expectedDtos);
        }

        @Test
        @DisplayName("관리자 권한이 아닌 경우 역할별 사용자 목록 조회에 실패한다")
        void listUsersByRole_NotAdmin_Fail() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> userController.listUsersByRole(UserRole.ADMIN))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("관리자 권한이 필요합니다.");

            verifyNoInteractions(userService);
        }
    }
}
