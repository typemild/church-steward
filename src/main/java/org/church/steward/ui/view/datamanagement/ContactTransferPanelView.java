package org.church.steward.ui.view.datamanagement;

import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.datatransfer.controller.ContactImportController;
import org.church.steward.domain.datatransfer.controller.DataExportController;
import org.church.steward.domain.datatransfer.controller.SpreadsheetFileController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.ui.view.datamanagement.contactimport.ContactImportPanel;

/**
 * 데이터 관리의 "관계자" 탭. 관계자(봉헌자/수취인)를 파일에서 가져오거나 파일로 내보낸다.
 * 관계자는 회계 연도에 속하지 않으므로 회계 연도 선택란이 없다.
 */
@UiView
public class ContactTransferPanelView extends AbstractTransferPanelView {

    private static final long serialVersionUID = 1L;

    private static final String EXPORT_DESCRIPTION = "등록된 관계자 전체(숨긴 관계자 포함)를 엑셀 또는 CSV 파일로 저장합니다.\n"
            + "관계자 하나가 한 행이며 구분, 이름, 비고, 상태(숨김/표시)를 적습니다.\n"
            + "봉헌자 정보가 들어 있으므로 암호를 건 엑셀 형식으로 저장하는 것을 권합니다.";
    private static final String IMPORT_DESCRIPTION = "엑셀 또는 CSV 파일의 관계자를 추가합니다.\n"
            + "파일의 형식은 정해져 있지 않으며, 어느 열이 구분과 이름인지 직접 지정합니다.\n"
            + "이미 있는 관계자는 바꾸지 않고, 가져올 내용을 확인한 뒤에 반영합니다.\n"
            + "양식은 머리글만 있는 빈 파일로, 가져올 관계자를 정리할 때 쓸 수 있습니다.";

    private final DataExportController dataExportController;
    private final ContactImportPanel contactImportPanel;

    public ContactTransferPanelView(DataExportController dataExportController,
            SpreadsheetFileController spreadsheetFileController, ContactImportController contactImportController,
            BackupController backupController) {
        super("관계자", "관계자", EXPORT_DESCRIPTION, IMPORT_DESCRIPTION);
        this.dataExportController = dataExportController;
        this.contactImportPanel = new ContactImportPanel(spreadsheetFileController, contactImportController,
                backupController, this::showHomePanel);
    }

    @Override
    protected int export(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        return dataExportController.exportContacts(target, format, password);
    }

    @Override
    protected void exportTemplate(Path target, SpreadsheetFormat format) {
        dataExportController.exportContactTemplate(target, format);
    }

    @Override
    protected void startImport(FiscalYearDto fiscalYear) {
        contactImportPanel.start(null);
        showImportPanel(contactImportPanel);
    }

    @Override
    protected void onTabActivated(List<FiscalYearDto> fiscalYears) {
        contactImportPanel.onReturned(fiscalYears);
    }

    @Override
    public boolean isImportInProgress() {
        return contactImportPanel.isInProgress();
    }

}
