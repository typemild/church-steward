package org.church.steward.domain.backup.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.church.steward.domain.backup.dto.BackupInfoDto;
import org.church.steward.domain.backup.service.BackupService;
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
class BackupControllerTest {

    private static final Path BACKUP_FILE = Path.of("backups", "backup.csbackup");

    @Mock
    private BackupService backupService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private BackupController backupController;

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("createBackup 테스트")
    class CreateBackupTest {

        @Test
        @DisplayName("서비스가 백업을 기록한 활동 기록의 ID를 그대로 반환한다")
        void createBackup_TargetFile_ReturnsServiceResult() {
            // Given
            when(backupService.createBackup(BACKUP_FILE)).thenReturn(42L);

            // When
            long logId = backupController.createBackup(BACKUP_FILE);

            // Then
            assertThat(logId).isEqualTo(42L);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void createBackup_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> backupController.createBackup(BACKUP_FILE))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(backupService);
        }
    }

    @Nested
    @DisplayName("inspectBackup 테스트")
    class InspectBackupTest {

        @Test
        @DisplayName("서비스가 읽은 백업 파일의 정보를 그대로 반환한다")
        void inspectBackup_BackupFile_ReturnsServiceResult() {
            // Given
            BackupInfoDto expected = BackupInfoDto.builder()
                    .build();
            when(backupService.inspectBackup(BACKUP_FILE)).thenReturn(expected);

            // When
            BackupInfoDto actual = backupController.inspectBackup(BACKUP_FILE);

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void inspectBackup_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> backupController.inspectBackup(BACKUP_FILE))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(backupService);
        }
    }

    @Nested
    @DisplayName("scheduleRestore 테스트")
    class ScheduleRestoreTest {

        @Test
        @DisplayName("복원할 백업 파일을 서비스에 그대로 넘긴다")
        void scheduleRestore_BackupFile_DelegatesToService() {
            // Given

            // When
            backupController.scheduleRestore(BACKUP_FILE);

            // Then
            verify(backupService).scheduleRestore(BACKUP_FILE);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void scheduleRestore_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> backupController.scheduleRestore(BACKUP_FILE))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(backupService);
        }
    }

}
