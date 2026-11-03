package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettlementDetailReportComposerTest {

    private SettlementDetailReportDto report(BudgetType type, int rowCount) {
        return report(type, rowCount, null);
    }

    /**
     * @param remarks 모든 예산의 비고. null이면 비고가 없다.
     */
    private SettlementDetailReportDto report(BudgetType type, int rowCount, String remarks) {
        List<BudgetExecutionDto> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(BudgetExecutionDto.builder()
                    .name("예산 " + i)
                    .depth(i % 2)
                    .budgetedAmount(1_000_000L)
                    .actualAmount(400_000L)
                    .shareRate(1.0)
                    .executionRate(40.0)
                    .remarks(remarks)
                    .build());
        }
        return SettlementDetailReportDto.builder()
                .fiscalYearName("2026년")
                .fiscalYearStartDate(LocalDate.of(2025, 12, 1))
                .fiscalYearEndDate(LocalDate.of(2026, 11, 30))
                .type(type)
                .rows(rows)
                .totalBudgetedAmount(rowCount * 1_000_000L)
                .totalActualAmount(rowCount * 400_000L)
                .totalExecutionRate(40.0)
                .build();
    }

    private static ReportTemplateDto template(BudgetType type) {
        return ReportTemplateFixtures.createDefaultTemplate(type == BudgetType.INCOME
                ? ReportType.INCOME_SETTLEMENT_DETAIL
                : ReportType.EXPENSE_SETTLEMENT_DETAIL);
    }

    private List<ReportElement.Text> textElementsOf(ReportDocument document) {
        return document.getElements(0)
                .stream()
                .filter(ReportElement.Text.class::isInstance)
                .map(ReportElement.Text.class::cast)
                .toList();
    }

    private List<String> textsOf(ReportDocument document) {
        return textElementsOf(document).stream()
                .map(ReportElement.Text::text)
                .toList();
    }

    private double xOf(ReportDocument document, String text) {
        return textElementsOf(document).stream()
                .filter(element -> element.text()
                        .equals(text))
                .findFirst()
                .orElseThrow()
                .x();
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
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.INCOME),
                    ReportStyle.DEFAULT);

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
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.EXPENSE),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 지출 결산 세부 내역서");
            assertThat(textsOf(document)).contains("집행액", "집행률");
        }

        @Test
        @DisplayName("제목 아래에 회계 연도의 기간을 적는다")
        void compose_Subtitle_WritesFiscalYearPeriod() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.INCOME),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("2025-12-01 ~ 2026-11-30");
        }

        @Test
        @DisplayName("기본 양식에서는 예산 항목, 예산액, 점유율, 수입액, 달성률, 비고 순으로 적는다")
        void compose_DefaultTemplate_WritesColumnsInDefaultOrder() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.INCOME),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(List.of(xOf(document, "예산 항목"), xOf(document, "예산액"), xOf(document, "점유율"),
                    xOf(document, "수입액"), xOf(document, "달성률"), xOf(document, "비고"))).isSorted();
        }

        @Test
        @DisplayName("칸에 들어가지 않는 비고는 예산 행 아래에 다시 적는다")
        void compose_LongRemarks_RewritesBelowRow() {
            // Given
            String remarks = "상반기에 행사가 취소되어 집행이 적었고 하반기에 수련회 비용을 한꺼번에 집행함 ".repeat(2)
                    .strip();
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 1, remarks);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.EXPENSE),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(String.join("", textsOf(document))
                    .replace(" ", "")).contains(("비고: " + remarks).replace(" ", ""));
        }

        @Test
        @DisplayName("한 장에 담을 수 없을 만큼 예산이 많으면 다음 페이지로 넘긴다")
        void compose_TooManyBudgets_BreaksIntoMultiplePages() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 200);

            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report, template(BudgetType.EXPENSE),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }
    }
}
