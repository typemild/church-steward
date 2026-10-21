package org.church.steward.domain.backup.service.impl;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.backup.dto.BackupInfoDto;
import org.church.steward.domain.backup.service.BackupService;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.backup.BackupManifest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BackupServiceImpl implements BackupService {

    private final BackupManager backupManager;
    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    public long createBackup(Path target) {
        List<String> userIds = userRepository.findAll()
                .stream()
                .map(User::getId)
                .toList();

        backupManager.createBackup(target, dataSource, userIds);

        // 백업에 성공한 뒤에만 기록한다. 그래서 이 기록 자체는 방금 만든 백업 파일에 들어 있지 않다.
        activityLogService.logBackup(target.getFileName().toString());
        return activityLogService.getLatestLogId();
    }

    @Override
    @Transactional(readOnly = true)
    public BackupInfoDto inspectBackup(Path file) {
        BackupManifest manifest = backupManager.inspect(file);
        return BackupInfoDto.builder()
                .createdAt(manifest.createdAt())
                .programVersion(manifest.programVersion())
                .build();
    }

    @Override
    public void scheduleRestore(Path file) {
        // 복원할 수 없는 파일을 예약하지 않도록 먼저 확인한다.
        backupManager.inspect(file);
        backupManager.scheduleRestore(file);
    }

    @Override
    public void logRestore(String userId, String fileName, LocalDateTime backupCreatedAt) {
        activityLogService.logRestore(userId, fileName, backupCreatedAt);
    }

}
