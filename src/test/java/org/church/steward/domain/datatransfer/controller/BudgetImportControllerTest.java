package org.church.steward.domain.datatransfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.service.BudgetImportService;
import org.church.steward.infrastructure.security.AccessDeniedException;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetImportControllerTest {

    private static final Long FISCAL_YEAR_ID = 7L;

    @Mock
    private BudgetImportService budgetImportService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private BudgetImportController budgetImportController;

    private final BudgetImportRequestDto request = BudgetImportRequestDto.builder()
            .build();

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("서비스가 분석한 결과를 그대로 반환한다")
        void analyze_ValidParameters_ReturnsServiceResult() {
            // Given
            BudgetImportAnalysisDto expected = BudgetImportAnalysisDto.builder()
                    .build();
            when(budgetImportService.analyze(FISCAL_YEAR_ID, request)).thenReturn(expected);

            // When
            BudgetImportAnalysisDto actual = budgetImportController.analyze(FISCAL_YEAR_ID, request);

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void analyze_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> budgetImportController.analyze(FISCAL_YEAR_ID, request))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(budgetImportService);
        }
    }

    @Nested
    @DisplayName("importBudgets 테스트")
    class ImportBudgetsTest {

        @Test
        @DisplayName("서비스가 반영한 결과를 그대로 반환한다")
        void importBudgets_ValidParameters_ReturnsServiceResult() {
            // Given
            Set<String> selectedKeys = Set.of("INCOME:헌금");
            ImportResultDto expected = ImportResultDto.builder()
                    .build();
            when(budgetImportService.importBudgets(FISCAL_YEAR_ID, request, selectedKeys, "signature", 10L,
                    "예산.xlsx")).thenReturn(expected);

            // When
            ImportResultDto actual = budgetImportController.importBudgets(FISCAL_YEAR_ID, request, selectedKeys,
                    "signature", 10L, "예산.xlsx");

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void importBudgets_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> budgetImportController.importBudgets(FISCAL_YEAR_ID, request,
                    Set.of("INCOME:헌금"), "signature", 10L, "예산.xlsx"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(budgetImportService);
        }
    }

}
