package org.church.steward.domain.datatransfer.controller;

import java.nio.file.Path;

import org.church.steward.domain.datatransfer.service.DataExportService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class DataExportController {

    private final DataExportService dataExportService;
    private final AdminAccessChecker adminAccessChecker;

    public int exportBudgets(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        adminAccessChecker.checkAdmin();
        return dataExportService.exportBudgets(fiscalYearId, target, format, password);
    }

    public int exportFinancialRecords(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        adminAccessChecker.checkAdmin();
        return dataExportService.exportFinancialRecords(fiscalYearId, target, format, password);
    }

    public void exportBudgetTemplate(Path target, SpreadsheetFormat format) {
        adminAccessChecker.checkAdmin();
        dataExportService.exportBudgetTemplate(target, format);
    }

    public void exportFinancialRecordTemplate(Path target, SpreadsheetFormat format) {
        adminAccessChecker.checkAdmin();
        dataExportService.exportFinancialRecordTemplate(target, format);
    }
}
