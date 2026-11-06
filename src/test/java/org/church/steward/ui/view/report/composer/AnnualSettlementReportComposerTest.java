package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Font;
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
import org.church.steward.ui.util.UIColors;
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

    private ReportElement.Text findText(ReportDocument document, String text) {
        return textElementsOf(document).stream()
                .filter(candidate -> candidate.text()
                        .equals(text))
                .findFirst()
                .orElseThrow();
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
        @DisplayName("문서 제목에는 회계 연도의 이름을 넣는다")
        void compose_Title_ContainsFiscalYearName() {
            // Given
            AnnualSettlementReportDto report = report(null, monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 결산 보고서");
        }

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
        @DisplayName("총괄 표는 내용에 맞춘 폭(금액 칸은 가장 큰 금액에 맞춘 같은 폭)에서 남는 폭을 네 칸이 똑같이 나눠 갖는다")
        void compose_Summary_SplitsRemainingWidthEvenly() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            // 표의 글자는 9pt이고 칸의 좌우 여백은 3pt다. 가장 넓은 내용은 굵게 적는 당해 행과 머리글이다.
            Font bold = ReportStyle.DEFAULT.bold(9f);
            double name = Math.max(ReportText.width("회계 연도", bold), ReportText.width(
                    "당해 (2026년, 2026-01-01 ~ 2026-12-31)", bold)) + 6;
            double amount = Math.max(ReportText.width("수입 총액", bold), ReportText.width("120,000,000", bold)) + 6;
            double contentWidth = ReportPaper.WIDTH - ReportPaper.MARGIN * 2;
            double extra = (contentWidth - name - amount * 3) / 4;
            assertThat(centerOf(document, "수입 총액")).isCloseTo(ReportPaper.MARGIN + name + extra + (amount + extra)
                    / 2, within(0.01));
        }

        @Test
        @DisplayName("총괄의 금액은 가장 큰 금액이 머리글과 같은 가운데에 오도록 덩어리로 칸 가운데에 놓는다")
        void compose_Summary_CentersAmountBlockUnderHeader() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(centerOf(document, "120,000,000")).isCloseTo(centerOf(document, "수입 총액"), within(0.01));
        }

        @Test
        @DisplayName("금액의 단위는 결재란 아래가 아니라 수입/지출 총괄 절 제목과 같은 줄의 오른쪽 끝에 적는다")
        void compose_Unit_WritesOnSummarySectionLine() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text unit = findText(document, "(단위: 원)");
            assertThat(unit.baseline()).isCloseTo(findText(document, "수입/지출 총괄").baseline(), within(0.01));
            assertThat(unit.x() + ReportText.width(unit.text(), unit.font())).isCloseTo(ReportPaper.WIDTH
                    - ReportPaper.MARGIN, within(0.01));
        }

        @Test
        @DisplayName("월별 표의 금액은 가장 큰 금액(합계)이 칸 가운데에 오도록 덩어리로 칸 가운데에 놓는다")
        void compose_Monthly_CentersAmountBlockInColumn() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(11_000_000L, 7_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            // 월별 표는 본문 폭의 48%로 오른쪽 끝에 놓이고, 월 칸(13mm)을 뺀 폭을 금액 세 칸이 똑같이 나눠 갖는다.
            double contentWidth = ReportPaper.WIDTH - ReportPaper.MARGIN * 2;
            double tableWidth = contentWidth * 0.48;
            double monthWidth = ReportPaper.mm(13);
            double amountWidth = (tableWidth - monthWidth) / 3;
            double incomeCenter = ReportPaper.MARGIN + contentWidth - tableWidth + monthWidth + amountWidth / 2;
            assertThat(centerOf(document, "132,000,000")).isCloseTo(incomeCenter, within(0.01));
        }

        @Test
        @DisplayName("현황 표의 금액은 가장 큰 금액(합계)이 머리글과 같은 가운데에 오도록 덩어리로 칸 가운데에 놓는다")
        void compose_Status_CentersAmountBlockUnderHeader() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            // 수입 현황은 예산 여덟 개가 4,000,000씩이므로 합계는 32,000,000이다.
            assertThat(centerOf(document, "32,000,000")).isCloseTo(centerOf(document, "수입액"), within(0.01));
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

    @Nested
    @DisplayName("compose 테스트 - 머리 부분")
    class ComposeHeaderTest {

        @Test
        @DisplayName("제목은 회계 연도 이름 없이 왼쪽에 적는다")
        void compose_Header_WritesTitleWithoutFiscalYearNameOnLeft() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).doesNotContain("2026년 회계 연도 결산 보고서");
            assertThat(findText(document, "회계 연도 결산 보고서").x()).isCloseTo(ReportPaper.MARGIN,
                    within(0.01));
        }

        @Test
        @DisplayName("제목 아래에 회계 연도의 이름과 기간을 한 줄씩 적는다")
        void compose_Header_WritesFiscalYearNameAndPeriodOnSeparateLines() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text title = findText(document, "회계 연도 결산 보고서");
            ReportElement.Text name = findText(document, "2026년");
            ReportElement.Text period = findText(document, "2026-01-01 ~ 2026-12-31");
            assertThat(textsOf(document)).contains("회계 연도 : ", "회계 기간 : ");
            assertThat(name.baseline()).isGreaterThan(title.baseline());
            assertThat(period.baseline()).isGreaterThan(name.baseline());
        }
    }

    @Nested
    @DisplayName("compose 테스트 - 결재란")
    class ComposeApprovalTest {

        private List<ReportElement.Rect> approvalBoxesOf(ReportDocument document, int cellCount) {
            double width = ReportPaper.mm(6) + ReportPaper.mm(20) * cellCount;
            return document.getElements(0)
                    .stream()
                    .filter(ReportElement.Rect.class::isInstance)
                    .map(ReportElement.Rect.class::cast)
                    .filter(rect -> UIColors.REPORT_LINE.equals(rect.stroke())
                            && Math.abs(rect.width() - width) < 0.01)
                    .toList();
        }

        @Test
        @DisplayName("기본 양식에서는 제목과 같은 높이의 오른쪽 끝에 세 칸의 결재란을 그린다")
        void compose_DefaultTemplate_DrawsThreeCellApprovalBesideTitleOnRight() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text title = findText(document, "회계 연도 결산 보고서");
            assertThat(approvalBoxesOf(document, 3)).singleElement()
                    .satisfies(box -> {
                        assertThat(box.x() + box.width()).isCloseTo(ReportPaper.WIDTH - ReportPaper.MARGIN,
                                within(0.01));
                        assertThat(box.y()).isLessThan(title.baseline());
                    });
        }

        @Test
        @DisplayName("결재란은 총괄 표보다 위에 놓인다")
        void compose_DefaultTemplate_DrawsApprovalAboveSummary() {
            // Given
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Rect box = approvalBoxesOf(document, 3).getFirst();
            ReportElement.Text summaryTitle = textElementsOf(document).stream()
                    .filter(text -> text.text()
                            .equals("수입/지출 총괄"))
                    .findFirst()
                    .orElseThrow();
            assertThat(summaryTitle.baseline()).isGreaterThan(box.y() + box.height());
        }

        @Test
        @DisplayName("양식에서 정한 칸 수와 직함으로 결재란을 그린다")
        void compose_ApprovalTitles_DrawsGivenCellsAndTitles() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.withApproval(DEFAULT_TEMPLATE,
                    ReportTemplateFixtures.createApproval(true, "담당", "재정부장", "감사", "담임목사"));
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument document = AnnualSettlementReportComposer.compose(report, template, ReportStyle.DEFAULT);

            // Then
            assertThat(approvalBoxesOf(document, 4)).hasSize(1);
            assertThat(textsOf(document)).contains("결", "재", "담당", "재정부장", "감사", "담임목사");
        }

        @Test
        @DisplayName("결재란을 숨기면 그리지 않지만 머리 부분의 높이는 결재란이 있을 때와 같다")
        void compose_ApprovalHidden_KeepsHeaderHeight() {
            // Given
            ReportTemplateDto hidden = ReportTemplateFixtures.withApproval(DEFAULT_TEMPLATE,
                    ReportTemplateFixtures.createApproval(false, null, null, null));
            AnnualSettlementReportDto report = report(previousSummary(), monthlyTrend(10_000_000L, 8_000_000L));

            // When
            ReportDocument visibleDocument = AnnualSettlementReportComposer.compose(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);
            ReportDocument hiddenDocument = AnnualSettlementReportComposer.compose(report, hidden,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(hiddenDocument)).doesNotContain("결", "재");
            assertThat(summaryBaseline(hiddenDocument)).isCloseTo(summaryBaseline(visibleDocument), within(0.01));
        }

        private double summaryBaseline(ReportDocument document) {
            return textElementsOf(document).stream()
                    .filter(text -> text.text()
                            .equals("수입/지출 총괄"))
                    .findFirst()
                    .orElseThrow()
                    .baseline();
        }
    }
}
