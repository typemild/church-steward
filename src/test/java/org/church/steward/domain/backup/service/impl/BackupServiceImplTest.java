package org.church.steward.domain.backup.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.backup.dto.BackupInfoDto;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.infrastructure.backup.BackupException;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.backup.BackupManifest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    private static final Path BACKUP_FILE = Path.of("backups", "청지기_백업_20261013_1530.csbackup");

    @Mock
    private BackupManager backupManager;
    @Mock
    private DataSource dataSource;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private BackupServiceImpl backupService;

    private User createUser(String id) {
        return User.builder()
                .id(id)
                .build();
    }

    @Nested
    @DisplayName("createBackup 테스트")
    class CreateBackupTest {

        @Test
        @DisplayName("등록된 모든 사용자의 ID를 넘겨 백업 파일을 만든다")
        void createBackup_RegisteredUsers_PassesAllUserIds() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(createUser("관리자"), createUser("홍길동")));

            // When
            backupService.createBackup(BACKUP_FILE);

            // Then
            verify(backupManager).createBackup(BACKUP_FILE, dataSource, List.of("관리자", "홍길동"));
        }

        @Test
        @DisplayName("백업에 성공하면 활동 기록에 백업 파일의 이름을 기록한다")
        void createBackup_Success_LogsFileName() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(createUser("관리자")));

            // When
            backupService.createBackup(BACKUP_FILE);

            // Then
            verify(activityLogService).logBackup("청지기_백업_20261013_1530.csbackup");
        }

        @Test
        @DisplayName("백업을 기록한 뒤의 가장 마지막 활동 기록 ID를 반환한다")
        void createBackup_Success_ReturnsLatestLogId() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(createUser("관리자")));
            when(activityLogService.getLatestLogId()).thenReturn(42L);

            // When
            long logId = backupService.createBackup(BACKUP_FILE);

            // Then
            assertThat(logId).isEqualTo(42L);
        }

        @Test
        @DisplayName("백업에 실패하면 활동 기록을 남기지 않는다")
        void createBackup_Failure_DoesNotLog() {
            // Given
            when(userRepository.findAll()).thenReturn(List.of(createUser("관리자")));
            when(backupManager.createBackup(any(), any(), any())).thenThrow(new BackupException("백업 실패"));

            // When
            catchThrowable(() -> backupService.createBackup(BACKUP_FILE));

            // Then
            verify(activityLogService, never()).logBackup(any());
        }
    }

    @Nested
    @DisplayName("inspectBackup 테스트")
    class InspectBackupTest {

        @Test
        @DisplayName("백업 파일에 기록된 일시와 프로그램 버전을 반환한다")
        void inspectBackup_ValidFile_ReturnsBackupInfo() {
            // Given
            LocalDateTime createdAt = LocalDateTime.of(2026, 10, 13, 15, 30, 0);
            when(backupManager.inspect(BACKUP_FILE)).thenReturn(new BackupManifest(1, "1.2.0", createdAt, List.of()));

            // When
            BackupInfoDto info = backupService.inspectBackup(BACKUP_FILE);

            // Then
            assertThat(info)
                    .extracting(BackupInfoDto::getCreatedAt, BackupInfoDto::getProgramVersion)
                    .containsExactly(createdAt, "1.2.0");
        }
    }

    @Nested
    @DisplayName("scheduleRestore 테스트")
    class ScheduleRestoreTest {

        @Test
        @DisplayName("복원할 수 있는 백업 파일이면 복원을 예약한다")
        void scheduleRestore_ValidFile_SchedulesRestore() {
            // Given
            when(backupManager.inspect(BACKUP_FILE))
                    .thenReturn(new BackupManifest(1, "1.2.0", LocalDateTime.of(2026, 10, 13, 15, 30, 0), List.of()));

            // When
            backupService.scheduleRestore(BACKUP_FILE);

            // Then
            verify(backupManager).scheduleRestore(BACKUP_FILE);
        }

        @Test
        @DisplayName("복원할 수 없는 백업 파일이면 예약하지 않는다")
        void scheduleRestore_InvalidFile_DoesNotSchedule() {
            // Given
            when(backupManager.inspect(BACKUP_FILE)).thenThrow(new BackupException("손상된 파일"));

            // When
            catchThrowable(() -> backupService.scheduleRestore(BACKUP_FILE));

            // Then
            verify(backupManager, never()).scheduleRestore(any());
        }
    }

    @Nested
    @DisplayName("logRestore 테스트")
    class LogRestoreTest {

        @Test
        @DisplayName("복원한 계정으로 백업 파일의 이름과 백업 일시를 활동 기록에 남긴다")
        void logRestore_RestoredBackup_LogsWithGivenUser() {
            // Given
            LocalDateTime backupCreatedAt = LocalDateTime.of(2026, 10, 13, 15, 30, 0);

            // When
            backupService.logRestore("관리자", "backup.csbackup", backupCreatedAt);

            // Then
            verify(activityLogService).logRestore("관리자", "backup.csbackup", backupCreatedAt);
        }
    }

}
