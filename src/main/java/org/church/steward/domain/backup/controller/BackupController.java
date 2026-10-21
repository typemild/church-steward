package org.church.steward.domain.backup.controller;

import java.nio.file.Path;

import org.church.steward.domain.backup.dto.BackupInfoDto;
import org.church.steward.domain.backup.service.BackupService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;
    private final AdminAccessChecker adminAccessChecker;

    public long createBackup(Path target) {
        adminAccessChecker.checkAdmin();
        return backupService.createBackup(target);
    }

    public BackupInfoDto inspectBackup(Path file) {
        adminAccessChecker.checkAdmin();
        return backupService.inspectBackup(file);
    }

    public void scheduleRestore(Path file) {
        adminAccessChecker.checkAdmin();
        backupService.scheduleRestore(file);
    }
}
