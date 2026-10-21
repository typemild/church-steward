package org.church.steward.domain.datatransfer.controller;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.datatransfer.service.RecordImportService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class RecordImportController {

    private final RecordImportService recordImportService;
    private final AdminAccessChecker adminAccessChecker;

    public RecordImportAnalysisDto analyze(Long fiscalYearId, RecordImportRequestDto request) {
        adminAccessChecker.checkAdmin();
        return recordImportService.analyze(fiscalYearId, request);
    }

    public ImportResultDto importRecords(Long fiscalYearId, RecordImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        adminAccessChecker.checkAdmin();
        return recordImportService.importRecords(fiscalYearId, request, selectedKeys, expectedSignature, backupLogId,
                fileName);
    }
}
