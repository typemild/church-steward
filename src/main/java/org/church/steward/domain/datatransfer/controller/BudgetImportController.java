package org.church.steward.domain.datatransfer.controller;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.service.BudgetImportService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class BudgetImportController {

    private final BudgetImportService budgetImportService;
    private final AdminAccessChecker adminAccessChecker;

    public BudgetImportAnalysisDto analyze(Long fiscalYearId, BudgetImportRequestDto request) {
        adminAccessChecker.checkAdmin();
        return budgetImportService.analyze(fiscalYearId, request);
    }

    public ImportResultDto importBudgets(Long fiscalYearId, BudgetImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        adminAccessChecker.checkAdmin();
        return budgetImportService.importBudgets(fiscalYearId, request, selectedKeys, expectedSignature, backupLogId,
                fileName);
    }
}
