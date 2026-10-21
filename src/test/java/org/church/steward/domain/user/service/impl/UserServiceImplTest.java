package org.church.steward.domain.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.domain.user.mapper.UserMapper;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.infrastructure.config.DatabaseManager;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.LoggedInUser;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSessionHolder userSessionHolder;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private EntitySnapshotCapturer entitySnapshotCapturer;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("createUser 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("createUser_중복되지 않은 ID로 추가 시 성공적으로 저장되어야 한다.")
        void createUser_Success() {
            // Given
            String id = "new_user";
            String rawPassword = "password123";
            String remarks = "신규 사용자";
            String encodedPassword = "encoded_password_abc";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            userService.createUser(id, rawPassword, remarks);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isEqualTo(id);
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(savedUser.getRemarks()).isEqualTo(remarks);
        }

        @Test
        @DisplayName("createUser_이미 존재하는 ID로 추가 시 IllegalArgumentException이 발생해야 한다.")
        void createUser_Fail_DuplicateId() {
            // Given
            String id = "existing_user";
            when(userRepository.existsById(id)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(id, "pw", "remarks"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 사용자 ID입니다");
        }

        @Test
        @DisplayName("createUser_성공 시 DatabaseManager의 비밀번호가 업데이트되어야 한다.")
        void createUser_Success_DatabaseManagerUpdated() throws Exception {
            // Given
            String id = "new_user";
            String rawPassword = "password123";
            String remarks = "신규 사용자";
            String encodedPassword = "encoded_password_abc";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            userService.createUser(id, rawPassword, remarks);

            // Then
            verify(databaseManager).saveOrUpdateUserPassword(id, rawPassword);
        }

        @Test
        @DisplayName("createUser_성공 시 활동 기록이 남아야 한다.")
        void createUser_Success_LogCreated() {
            // Given
            String id = "new_user";
            String rawPassword = "password123";
            String remarks = "신규 사용자";
            String encodedPassword = "encoded_password_abc";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
            // userRepository.save()는 내부적으로 호출되므로 verify 대상에서 제외하거나
            // 행위 검증 분리를 위해 별도 테스트로 구성

            // When
            userService.createUser(id, rawPassword, remarks);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(activityLogService).logCreation(userCaptor.capture());

            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getId()).isEqualTo(id);
            assertThat(capturedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(capturedUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(capturedUser.getRemarks()).isEqualTo(remarks);
        }
    }

    @Nested
    @DisplayName("ID 검증 테스트")
    class IdValidationTest {

        @Test
        @DisplayName("createUser_유효한 ID로 생성 시 성공해야 한다.")
        void createUser_Success_ValidId() {
            // Given
            String id = "user_123";
            String rawPassword = "password123";
            String remarks = "신규 사용자";
            String encodedPassword = "encoded_password_abc";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            userService.createUser(id, rawPassword, remarks);

            // Then
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("createUser_한글 ID로 생성 시 성공해야 한다.")
        void createUser_Success_KoreanId() {
            // Given
            String id = "홍길동";
            String rawPassword = "password123";
            String remarks = "신규 사용자";
            String encodedPassword = "encoded_password_abc";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            userService.createUser(id, rawPassword, remarks);

            // Then
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("createUser_숫자로 시작하는 ID로 생성 시 IllegalArgumentException이 발생해야 한다.")
        void createUser_Fail_IdStartsWithNumber() {
            // Given
            String id = "1user";

            // When & Then
            assertThatThrownBy(() -> userService.createUser(id, "pw", "remarks"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("첫 글자는 한글 또는 영문이어야 합니다");
        }

        @Test
        @DisplayName("createUser_특수문자가 포함된 ID로 생성 시 IllegalArgumentException이 발생해야 한다.")
        void createUser_Fail_IdWithSpecialChar() {
            // Given
            String id = "user-123";

            // When & Then
            assertThatThrownBy(() -> userService.createUser(id, "pw", "remarks"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("한글, 영문, 숫자로 구성되어야 하며");
        }

        @Test
        @DisplayName("createUser_빈 ID로 생성 시 IllegalArgumentException이 발생해야 한다.")
        void createUser_Fail_EmptyId() {
            // Given
            String id = "";

            // When & Then
            assertThatThrownBy(() -> userService.createUser(id, "pw", "remarks"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수 입력 사항입니다");
        }
    }

    @Nested
    @DisplayName("createAdmin 테스트")
    class CreateAdminTest {

        @Test
        @DisplayName("createAdmin_중복되지 않은 ID로 추가 시 성공적으로 ADMIN 권한으로 저장되어야 한다.")
        void createAdmin_Success() {
            // Given
            String id = "new_admin";
            String rawPassword = "admin_password";
            String remarks = "신규 관리자";
            String encodedPassword = "encoded_admin_password";

            when(userRepository.existsById(id)).thenReturn(false);
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            userService.createAdmin(id, rawPassword, remarks);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getId()).isEqualTo(id);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("createAdmin_이미 존재하는 ID로 추가 시 IllegalArgumentException이 발생해야 한다.")
        void createAdmin_Fail_DuplicateId() {
            // Given
            String id = "existing_admin";
            when(userRepository.existsById(id)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createAdmin(id, "pw", "remarks"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 사용자 ID입니다");
        }
    }

    @Nested
    @DisplayName("login 테스트")
    class LoginTest {

        @Test
        @DisplayName("login_올바른 ID와 비밀번호로 로그인 시 성공하고 세션에 저장되어야 한다.")
        void login_Success_SessionSaved() {
            // Given
            String id = "user1";
            String rawPassword = "password123";
            String encodedPassword = "encoded_password";
            User user = User.builder().id(id).password(encodedPassword).role(UserRole.USER).build();

            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

            // When
            User result = userService.login(id, rawPassword);

            // Then
            assertThat(result).isEqualTo(user);
            verify(userSessionHolder).setLoggedInUser(new LoggedInUser(id, UserRole.USER));
        }

        @Test
        @DisplayName("login_올바른 ID와 비밀번호로 로그인 시 로그인 로그가 기록되어야 한다.")
        void login_Success_LogRecorded() {
            // Given
            String id = "user1";
            String rawPassword = "password123";
            String encodedPassword = "encoded_password";
            User user = User.builder().id(id).password(encodedPassword).build();

            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

            // When
            userService.login(id, rawPassword);

            // Then
            verify(activityLogService).logLogin(id);
        }

        @Test
        @DisplayName("login_존재하지 않는 ID로 로그인 시 UserNotFoundException이 발생해야 한다.")
        void login_Fail_UserNotFound() {
            // Given
            String id = "nonexistent";
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.login(id, "pw")).isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("login_잘못된 비밀번호로 로그인 시 InvalidPasswordException이 발생해야 한다.")
        void login_Fail_WrongPassword() {
            // Given
            String id = "user1";
            String rawPassword = "wrong_password";
            String encodedPassword = "encoded_password";
            User user = User.builder().id(id).password(encodedPassword).build();

            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.login(id, rawPassword)).isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("비밀번호가 일치하지 않습니다");
        }
    }

    @Nested
    @DisplayName("updateUserPassword 테스트")
    class UpdateUserPasswordTest {

        @Test
        @DisplayName("updateUserPassword_올바른 기존 비밀번호와 새 비밀번호로 변경 시 성공해야 한다.")
        void updateUserPassword_Success() throws Exception {
            // Given
            String userId = "user1";
            String oldPassword = "old_password";
            String newPassword = "new_password";
            String encodedOldPassword = "encoded_for_old";
            String encodedNewPassword = "encoded_for_new";
            User user = User.builder().id(userId).password(encodedOldPassword).build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUserPassword(userId, oldPassword, newPassword);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(encodedNewPassword);
        }

        @Test
        @DisplayName("updateUserPassword_성공 시 DatabaseManager의 비밀번호가 업데이트되어야 한다.")
        void updateUserPassword_Success_DatabaseManagerUpdated() throws Exception {
            // Given
            String userId = "user1";
            String oldPassword = "old_password";
            String newPassword = "new_password";
            String encodedOldPassword = "encoded_for_old";
            String encodedNewPassword = "encoded_for_new";
            User user = User.builder().id(userId).password(encodedOldPassword).build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUserPassword(userId, oldPassword, newPassword);

            // Then
            verify(databaseManager).saveOrUpdateUserPassword(userId, newPassword);
        }

        @Test
        @DisplayName("updateUserPassword_기존 비밀번호가 일치하지 않으면 IllegalArgumentException이 발생해야 한다.")
        void updateUserPassword_Fail_WrongOldPassword() {
            // Given
            String userId = "user1";
            String oldPassword = "wrong_old_password";
            String newPassword = "new_password";
            String encodedOldPassword = "encoded_for_old";
            User user = User.builder().id(userId).password(encodedOldPassword).build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.updateUserPassword(userId, oldPassword, newPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
        }
    }

    @Nested
    @DisplayName("updateUser 테스트")
    class UpdateUserTest {

        @Test
        @DisplayName("updateUser_비밀번호와 비고를 모두 제공 시 성공적으로 업데이트되어야 한다.")
        void updateUser_Success_AllFields() {
            // Given
            String userId = "user1";
            String newPassword = "new_password";
            String remarks = "수정된 비고";
            String encodedPassword = "encoded_new_password";
            User user = User.builder().id(userId).password("old_password").remarks("old_remarks").build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUser(userId, newPassword, remarks);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertThat(updatedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(updatedUser.getRemarks()).isEqualTo(remarks);
        }

        @Test
        @DisplayName("updateUser_비밀번호를 제공하지 않은 경우 비고만 업데이트되어야 한다.")
        void updateUser_Success_OnlyRemarks() {
            // Given
            String userId = "user1";
            String newPassword = null;
            String remarks = "비고만 수정";
            User user = User.builder().id(userId).password("old_password").remarks("old_remarks").build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUser(userId, newPassword, remarks);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertThat(updatedUser.getPassword()).isEqualTo("old_password");
            assertThat(updatedUser.getRemarks()).isEqualTo(remarks);
        }

        @Test
        @DisplayName("updateUser_성공 시 활동 기록이 남아야 한다.")
        void updateUser_Success_LogUpdated() {
            // Given
            String userId = "user1";
            String newPassword = null;
            String remarks = "비고 수정";
            User user = User.builder().id(userId).password("old_password").remarks("old_remarks").build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUser(userId, newPassword, remarks);

            // Then
            verify(activityLogService).logUpdate(oldSnapshot, user);
        }

        @Test
        @DisplayName("updateUser_비밀번호 제공 시 DatabaseManager의 비밀번호가 업데이트되어야 한다.")
        void updateUser_Success_DatabaseManagerUpdated() throws Exception {
            // Given
            String userId = "user1";
            String newPassword = "new_password";
            String remarks = "수정된 비고";
            String encodedPassword = "encoded_new_password";
            User user = User.builder().id(userId).password("old_password").remarks("old_remarks").build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<User> oldSnapshot = mock(EntitySnapshot.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(entitySnapshotCapturer.capture(user)).thenReturn(oldSnapshot);

            // When
            userService.updateUser(userId, newPassword, remarks);

            // Then
            verify(databaseManager).saveOrUpdateUserPassword(userId, newPassword);
        }
    }

    @Nested
    @DisplayName("deleteUser 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("deleteUser_사용자가 존재하면 성공적으로 삭제되어야 한다.")
        void deleteUser_Success() {
            // Given
            String userId = "user1";
            User user = User.builder().id(userId).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            userService.deleteUser(userId);

            // Then
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("deleteUser_사용자가 존재하지 않으면 IllegalArgumentException이 발생해야 한다.")
        void deleteUser_Fail_UserNotFound() {
            // Given
            String userId = "nonexistent";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("deleteUser_성공 시 활동 기록이 남아야 한다.")
        void deleteUser_Success_LogDeleted() {
            // Given
            String userId = "user1";
            User user = User.builder().id(userId).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            userService.deleteUser(userId);

            // Then
            verify(activityLogService).logDeletion(user);
        }

        @Test
        @DisplayName("deleteUser_성공 시 DatabaseManager의 비밀번호가 삭제되어야 한다.")
        void deleteUser_Success_DatabaseManagerRemoved() throws Exception {
            // Given
            String userId = "user1";
            User user = User.builder().id(userId).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            userService.deleteUser(userId);

            // Then
            verify(databaseManager).removeUserPassword(userId);
        }
    }

    @Nested
    @DisplayName("listUsersByRole 테스트")
    class ListUsersByRoleTest {

        @Test
        @DisplayName("listUsersByRole_해당 역할의 사용자를 성공적으로 목록으로 반환해야 한다.")
        void listUsersByRole_Success() {
            // Given
            User user1 = User.builder().id("user1").build();
            User user2 = User.builder().id("user2").build();

            UserDto dto1 = UserDto.builder().id("user1").build();
            UserDto dto2 = UserDto.builder().id("user2").build();

            when(userRepository.findByRole(UserRole.USER)).thenReturn(List.of(user1, user2));
            when(userMapper.toDto(user1)).thenReturn(dto1);
            when(userMapper.toDto(user2)).thenReturn(dto2);

            // When
            List<UserDto> result = userService.listUsersByRole(UserRole.USER);

            // Then
            assertThat(result).hasSize(2)
                    .containsExactlyInAnyOrder(dto1, dto2);
        }
    }
}
