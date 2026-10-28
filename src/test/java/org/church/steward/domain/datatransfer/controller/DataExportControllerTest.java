package org.church.steward.domain.datatransfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.church.steward.domain.datatransfer.service.DataExportService;
import org.church.steward.infrastructure.security.AccessDeniedException;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportControllerTest {

    private static final Long FISCAL_YEAR_ID = 7L;
    private static final Path TARGET = Path.of("exports", "export.xlsx");

    @Mock
    private DataExportService dataExportService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private DataExportController dataExportController;

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("exportBudgets 테스트")
    class ExportBudgetsTest {

        @Test
        @DisplayName("서비스가 내보낸 예산의 수를 그대로 반환한다")
        void exportBudgets_ValidParameters_ReturnsServiceResult() {
            // Given
            when(dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, "secret"))
                    .thenReturn(12);

            // When
            int count = dataExportController.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, "secret");

            // Then
            assertThat(count).isEqualTo(12);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportBudgets_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportBudgets(FISCAL_YEAR_ID, TARGET,
                    SpreadsheetFormat.XLSX, "secret"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

    @Nested
    @DisplayName("exportFinancialRecords 테스트")
    class ExportFinancialRecordsTest {

        @Test
        @DisplayName("서비스가 내보낸 내역의 수를 그대로 반환한다")
        void exportFinancialRecords_ValidParameters_ReturnsServiceResult() {
            // Given
            when(dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.CSV, null))
                    .thenReturn(345);

            // When
            int count = dataExportController.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.CSV,
                    null);

            // Then
            assertThat(count).isEqualTo(345);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportFinancialRecords_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportFinancialRecords(FISCAL_YEAR_ID, TARGET,
                    SpreadsheetFormat.CSV, null))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

    @Nested
    @DisplayName("exportContacts 테스트")
    class ExportContactsTest {

        @Test
        @DisplayName("서비스가 내보낸 관계자의 수를 그대로 반환한다")
        void exportContacts_ValidParameters_ReturnsServiceResult() {
            // Given
            when(dataExportService.exportContacts(TARGET, SpreadsheetFormat.XLSX, "secret")).thenReturn(24);

            // When
            int count = dataExportController.exportContacts(TARGET, SpreadsheetFormat.XLSX, "secret");

            // Then
            assertThat(count).isEqualTo(24);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportContacts_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportContacts(TARGET, SpreadsheetFormat.XLSX, "secret"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

    @Nested
    @DisplayName("exportBudgetTemplate 테스트")
    class ExportBudgetTemplateTest {

        @Test
        @DisplayName("저장할 파일과 형식을 서비스에 그대로 넘긴다")
        void exportBudgetTemplate_TargetAndFormat_DelegatesToService() {
            // Given

            // When
            dataExportController.exportBudgetTemplate(TARGET, SpreadsheetFormat.XLSX);

            // Then
            verify(dataExportService).exportBudgetTemplate(TARGET, SpreadsheetFormat.XLSX);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportBudgetTemplate_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportBudgetTemplate(TARGET, SpreadsheetFormat.XLSX))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

    @Nested
    @DisplayName("exportFinancialRecordTemplate 테스트")
    class ExportFinancialRecordTemplateTest {

        @Test
        @DisplayName("저장할 파일과 형식을 서비스에 그대로 넘긴다")
        void exportFinancialRecordTemplate_TargetAndFormat_DelegatesToService() {
            // Given

            // When
            dataExportController.exportFinancialRecordTemplate(TARGET, SpreadsheetFormat.XLSX);

            // Then
            verify(dataExportService).exportFinancialRecordTemplate(TARGET, SpreadsheetFormat.XLSX);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportFinancialRecordTemplate_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportFinancialRecordTemplate(TARGET,
                    SpreadsheetFormat.XLSX))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

    @Nested
    @DisplayName("exportContactTemplate 테스트")
    class ExportContactTemplateTest {

        @Test
        @DisplayName("저장할 파일과 형식을 서비스에 그대로 넘긴다")
        void exportContactTemplate_TargetAndFormat_DelegatesToService() {
            // Given

            // When
            dataExportController.exportContactTemplate(TARGET, SpreadsheetFormat.CSV);

            // Then
            verify(dataExportService).exportContactTemplate(TARGET, SpreadsheetFormat.CSV);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void exportContactTemplate_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> dataExportController.exportContactTemplate(TARGET, SpreadsheetFormat.CSV))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(dataExportService);
        }
    }

}
