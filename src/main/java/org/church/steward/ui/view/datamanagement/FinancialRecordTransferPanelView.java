package org.church.steward.ui.view.datamanagement;

import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.datatransfer.controller.DataExportController;
import org.church.steward.domain.datatransfer.controller.RecordImportController;
import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.domain.fiscalyear.controller.FiscalYearController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportPanel;
import org.church.steward.ui.view.main.FiscalYearSelector;

/**
 * 데이터 관리의 "수입/지출" 탭. 회계 연도를 골라 수입/지출 내역을 파일에서 가져오거나 파일로 내보낸다.
 */
@UiView
public class FinancialRecordTransferPanelView extends AbstractTransferPanelView {

    private static final long serialVersionUID = 1L;

    private static final String EXPORT_DESCRIPTION = "선택한 회계 연도의 수입/지출 내역 전체를 엑셀 또는 CSV 파일로 저장합니다.\n"
            + "내역 하나가 한 행이며 구분, 일자, 단계별 예산명, 금액, 내용, 봉헌자/수취인, 비고를 적습니다.\n"
            + "봉헌자 정보가 들어 있으므로 암호를 건 엑셀 형식으로 저장하는 것을 권합니다.";
    private static final String IMPORT_DESCRIPTION = "엑셀 또는 CSV 파일의 수입/지출 내역을 선택한 회계 연도에 추가합니다.\n"
            + "예산과 관계자는 새로 만들지 않고, 파일의 값을 등록된 예산과 관계자에 매핑합니다.\n"
            + "이미 있는 내역은 바꾸지 않으며, 가져올 내용을 확인한 뒤에 반영합니다.\n"
            + "양식은 머리글만 있는 빈 파일로, 가져올 내역을 정리할 때 쓸 수 있습니다.";

    private final DataExportController dataExportController;
    private final RecordImportPanel recordImportPanel;

    public FinancialRecordTransferPanelView(DataExportController dataExportController,
            SpreadsheetFileController spreadsheetFileController, RecordImportController recordImportController,
            BackupController backupController, BudgetController budgetController,
            ContactController contactController, ContactGroupController contactGroupController,
            FiscalYearController fiscalYearController, FiscalYearSelector fiscalYearSelector) {
        super("수입/지출", "수입지출", EXPORT_DESCRIPTION, IMPORT_DESCRIPTION, fiscalYearController,
                fiscalYearSelector);
        this.dataExportController = dataExportController;
        this.recordImportPanel = new RecordImportPanel(spreadsheetFileController, recordImportController,
                backupController, budgetController, contactController, contactGroupController,
                this::showHomePanel);
    }

    @Override
    protected int export(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        return dataExportController.exportFinancialRecords(fiscalYearId, target, format, password);
    }

    @Override
    protected void exportTemplate(Path target, SpreadsheetFormat format) {
        dataExportController.exportFinancialRecordTemplate(target, format);
    }

    @Override
    protected void startImport(FiscalYearDto fiscalYear) {
        recordImportPanel.start(fiscalYear);
        showImportPanel(recordImportPanel);
    }

    @Override
    protected void onFiscalYearsReloaded(List<FiscalYearDto> fiscalYears) {
        recordImportPanel.onReturned(fiscalYears);
    }

    @Override
    public boolean isImportInProgress() {
        return recordImportPanel.isInProgress();
    }

}
