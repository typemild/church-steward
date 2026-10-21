package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettlementDetailReportComposerTest {

    private SettlementDetailReportDto report(BudgetType type, int rowCount) {
        List<BudgetExecutionDto> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(BudgetExecutionDto.builder()
                    .name("예산 " + i)
                    .depth(i % 2)
                    .budgetedAmount(1_000_000L)
                    .actualAmount(400_000L)
                    .shareRate(1.0)
                    .executionRate(40.0)
                    .build());
        }
        return SettlementDetailReportDto.builder()
                .fiscalYearName("2026년")
                .type(type)
                .rows(rows)
                .totalBudgetedAmount(rowCount * 1_000_000L)
                .totalActualAmount(rowCount * 400_000L)
                .totalExecutionRate(40.0)
                .build();
    }

    private List<String> textsOf(ReportDocument document) {
        return document.getElements(0)
                .stream()
                .filter(ReportElement.Text.class::isInstance)
                .map(element -> ((ReportElement.Text) element).text())
                .toList();
    }

    @Nested
    @DisplayName("compose 테스트")
    class ComposeTest {

        @Test
        @DisplayName("수입 내역서는 실제 금액을 수입액, 비율을 달성률로 부른다")
        void compose_IncomeReport_UsesIncomeColumnNames() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 수입 결산 세부 내역서");
            assertThat(textsOf(document)).contains("수입액", "달성률");
        }

        @Test
        @DisplayName("지출 내역서는 실제 금액을 집행액, 비율을 집행률로 부른다")
        void compose_ExpenseReport_UsesExpenseColumnNames() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 3);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 지출 결산 세부 내역서");
            assertThat(textsOf(document)).contains("집행액", "집행률");
        }

        @Test
        @DisplayName("한 장에 담을 수 없을 만큼 예산이 많으면 다음 쪽으로 넘긴다")
        void compose_TooManyBudgets_BreaksIntoMultiplePages() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 200);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }
    }
}
