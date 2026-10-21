package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnnualSettlementReportComposerTest {

    private List<MonthlyTrendDto> monthlyTrend(long income, long expense) {
        List<MonthlyTrendDto> trend = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            trend.add(MonthlyTrendDto.builder()
                    .yearMonth(String.format("2026-%02d", month))
                    .income(income)
                    .expense(expense)
                    .build());
        }
        return trend;
    }

    private List<BudgetExecutionDto> status(int count) {
        List<BudgetExecutionDto> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(BudgetExecutionDto.builder()
                    .name("예산 " + i)
                    .budgetedAmount(10_000_000L)
                    .actualAmount(4_000_000L)
                    .executionRate(40.0)
                    .build());
        }
        return rows;
    }

    /**
     * 회계 연도 "2026년"의 보고서 데이터를 만든다.
     *
     * @param previousYear 직전 회계 연도 "2025년"의 총액. null이면 직전 회계 연도가 없는 것으로 한다.
     */
    private AnnualSettlementReportDto report(ReportSummaryDto previousYear, List<MonthlyTrendDto> trend) {
        return AnnualSettlementReportDto.builder()
                .fiscalYearName("2026년")
                .previousFiscalYearName(previousYear == null ? null : "2025년")
                .previousYear(previousYear)
                .currentYear(ReportSummaryDto.builder()
                        .income(120_000_000L)
                        .expense(96_000_000L)
                        .build())
                .monthlyTrend(trend)
                .incomeStatus(status(8))
                .expenseStatus(status(12))
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
        @DisplayName("총괄, 월별 차트와 표, 최상위 예산 십여 개의 현황을 A4 한 장에 담는다")
        void compose_TypicalYear_FitsOnSinglePage() {
            // Given
            AnnualSettlementReportDto report = report(ReportSummaryDto.builder()
                    .income(100_000_000L)
                    .expense(90_000_000L)
                    .build(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("제목에 회계 연도의 이름을 넣는다")
        void compose_Title_ContainsFiscalYearName() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 수입/지출 결산 보고서");
        }

        @Test
        @DisplayName("총괄에 직전 회계 연도와 해당 회계 연도를 이름과 함께 적는다")
        void compose_PreviousYearExists_WritesBothFiscalYearNames() {
            // Given
            AnnualSettlementReportDto report = report(ReportSummaryDto.builder()
                    .income(100_000_000L)
                    .expense(90_000_000L)
                    .build(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(textsOf(document)).contains("직전 회계 연도 (2025년)", "해당 회계 연도 (2026년)");
        }

        @Test
        @DisplayName("직전 회계 연도가 없으면 이름 대신 없음으로 적는다")
        void compose_NoPreviousYear_WritesNoneAsPreviousFiscalYearName() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(textsOf(document)).contains("직전 회계 연도 (없음)");
        }

        @Test
        @DisplayName("해당 회계 연도의 수입 총액, 지출 총액, 차액을 적는다")
        void compose_CurrentYearSummary_WritesIncomeExpenseAndDifference() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(textsOf(document)).contains("120,000,000", "96,000,000", "24,000,000");
        }

        @Test
        @DisplayName("수입과 지출이 전혀 없으면 차트 대신 안내 문구를 적는다")
        void compose_NoRecords_WritesEmptyChartMessage() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(0L, 0L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report);

            // Then
            assertThat(textsOf(document)).contains("등록된 수입/지출이 없습니다.");
        }
    }
}
