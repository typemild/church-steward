package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnnualSettlementReportComposerTest {

    private static final ReportTemplateDto DEFAULT_TEMPLATE = ReportTemplateFixtures.createDefaultTemplate(
            ReportType.ANNUAL_SETTLEMENT);
    /** 현황 표 하나의 폭. 수입 현황과 지출 현황이 본문 폭을 나눠 갖는다. */
    private static final double STATUS_TABLE_WIDTH = (ReportPaper.WIDTH - ReportPaper.MARGIN * 2
            - BudgetStatusSection.COLUMN_GAP) / 2;

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
                .fiscalYearStartDate(LocalDate.of(2026, 1, 1))
                .fiscalYearEndDate(LocalDate.of(2026, 12, 31))
                .previousFiscalYearName(previousYear == null ? null : "2025년")
                .previousFiscalYearStartDate(previousYear == null ? null : LocalDate.of(2025, 1, 1))
                .previousFiscalYearEndDate(previousYear == null ? null : LocalDate.of(2025, 12, 31))
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

    private ReportSummaryDto previousSummary() {
        return ReportSummaryDto.builder()
                .income(100_000_000L)
                .expense(90_000_000L)
                .build();
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

    private double centerOf(ReportDocument document, String text) {
        ReportElement.Text element = textElementsOf(document).stream()
                .filter(candidate -> candidate.text()
                        .equals(text))
                .findFirst()
                .orElseThrow();
        return element.x() + ReportText.width(element.text(), element.font()) / 2;
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
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("제목에 회계 연도의 이름을 넣는다")
        void compose_Title_ContainsFiscalYearName() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 수입/지출 결산 보고서");
        }

        @Test
        @DisplayName("총괄에 직전 회계 연도와 해당 회계 연도를 이름, 기간과 함께 한 줄에 적는다")
        void compose_PreviousYearExists_WritesBothFiscalYearNamesAndPeriods() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("회계 연도", "직전 (2025년, 2025-01-01 ~ 2025-12-31)",
                    "당해 (2026년, 2026-01-01 ~ 2026-12-31)");
        }

        @Test
        @DisplayName("직전 회계 연도가 없으면 이름과 기간 대신 없음으로 적는다")
        void compose_NoPreviousYear_WritesNoneAsPreviousFiscalYearName() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("직전 (없음)");
        }

        @Test
        @DisplayName("총괄의 금액 칸은 가장 큰 금액에 맞춰 좁히고 회계 연도 칸이 남은 폭을 가져간다")
        void compose_Summary_FitsAmountColumnsToLargestAmount() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            double contentWidth = ReportPaper.WIDTH - ReportPaper.MARGIN * 2;
            assertThat(centerOf(document, "수입 총액")).isGreaterThan(ReportPaper.MARGIN + contentWidth * 0.6);
        }

        @Test
        @DisplayName("현황 표의 막대를 숨겨도 비율 칸은 고른 폭을 그대로 갖고 비율을 그 가운데에 적는다")
        void compose_StatusBarHidden_KeepsRateColumnWidth() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.createTemplate(ReportType.ANNUAL_SETTLEMENT,
                    Map.of(), false);
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, template, ReportStyle.DEFAULT);

            // Then
            assertThat(centerOf(document, "달성률")).isCloseTo(ReportPaper.MARGIN + STATUS_TABLE_WIDTH * 7 / 8,
                    within(0.01));
        }

        @Test
        @DisplayName("현황 표는 양식에서 바꾼 칸 순서대로 적는다")
        void compose_ReorderedStatus_FollowsTemplateOrder() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.createTemplate(ReportType.ANNUAL_SETTLEMENT,
                    Map.of(ReportTemplateTable.ANNUAL_INCOME_STATUS, ReportTemplateFixtures.createColumns(List.of(
                            ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.ACTUAL_AMOUNT,
                            ReportColumn.RATE))),
                    true);
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, template, ReportStyle.DEFAULT);

            // Then
            assertThat(centerOf(document, "예산액")).isLessThan(centerOf(document, "수입액"));
        }

        @Test
        @DisplayName("해당 회계 연도의 수입 총액, 지출 총액, 차액을 적는다")
        void compose_CurrentYearSummary_WritesIncomeExpenseAndDifference() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("120,000,000", "96,000,000", "24,000,000");
        }

        @Test
        @DisplayName("수입과 지출이 전혀 없으면 차트 대신 안내 문구를 적는다")
        void compose_NoRecords_WritesEmptyChartMessage() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(0L, 0L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("등록된 수입/지출이 없습니다.");
        }
    }
}
