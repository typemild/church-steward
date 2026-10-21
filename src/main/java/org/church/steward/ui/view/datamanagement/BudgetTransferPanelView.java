package org.church.steward.ui.view.datamanagement;

import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.datatransfer.controller.BudgetImportController;
import org.church.steward.domain.datatransfer.controller.DataExportController;
import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.domain.fiscalyear.controller.FiscalYearController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.ui.view.datamanagement.budgetimport.BudgetImportPanel;
import org.church.steward.ui.view.main.FiscalYearSelector;

/**
 * 데이터 관리의 "예산" 탭. 회계 연도를 골라 예산을 파일에서 가져오거나 파일로 내보낸다.
 */
@UiView
public class BudgetTransferPanelView extends AbstractTransferPanelView {

    private static final long serialVersionUID = 1L;

    private static final String EXPORT_DESCRIPTION = "선택한 회계 연도의 예산 전체를 엑셀 또는 CSV 파일로 저장합니다.\n"
            + "예산 하나가 한 행이며 구분, 단계별 예산명, 예산액, 수입액/집행액을 적습니다.";
    private static final String IMPORT_DESCRIPTION = "엑셀 또는 CSV 파일의 예산을 선택한 회계 연도에 추가합니다.\n"
            + "파일의 형식은 정해져 있지 않으며, 어느 열이 예산명과 금액인지 직접 지정합니다.\n"
            + "이미 있는 예산은 지우지 않고, 가져올 내용을 확인한 뒤에 반영합니다.\n"
            + "양식은 머리글만 있는 빈 파일로, 가져올 예산을 정리할 때 쓸 수 있습니다.";

    private final DataExportController dataExportController;
    private final BudgetImportPanel budgetImportPanel;

    public BudgetTransferPanelView(DataExportController dataExportController,
            SpreadsheetFileController spreadsheetFileController, BudgetImportController budgetImportController,
            BackupController backupController, FiscalYearController fiscalYearController,
            FiscalYearSelector fiscalYearSelector) {
        super("예산", "예산", EXPORT_DESCRIPTION, IMPORT_DESCRIPTION, fiscalYearController, fiscalYearSelector);
        this.dataExportController = dataExportController;
        this.budgetImportPanel = new BudgetImportPanel(spreadsheetFileController, budgetImportController,
                backupController, this::showHomePanel);
    }

    @Override
    protected int export(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        return dataExportController.exportBudgets(fiscalYearId, target, format, password);
    }

    @Override
    protected void exportTemplate(Path target, SpreadsheetFormat format) {
        dataExportController.exportBudgetTemplate(target, format);
    }

    @Override
    protected void startImport(FiscalYearDto fiscalYear) {
        budgetImportPanel.start(fiscalYear);
        showImportPanel(budgetImportPanel);
    }

    @Override
    protected void onFiscalYearsReloaded(List<FiscalYearDto> fiscalYears) {
        budgetImportPanel.onReturned(fiscalYears);
    }

    @Override
    public boolean isImportInProgress() {
        return budgetImportPanel.isInProgress();
    }

}
