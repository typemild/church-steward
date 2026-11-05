package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SettlementDetailReportComposerTest {

    private static final ReportTemplateDto DEFAULT_TEMPLATE = ReportTemplateFixtures.createDefaultTemplate(
            ReportType.SETTLEMENT_DETAIL);

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

    /**
     * 내역서 하나만 배치한 문서. 수입과 지출을 묶지 않고 배치해 그 내역서의 배치만 본다.
     */
    private ReportDocument drawAlone(SettlementDetailReportDto report, ReportTemplateDto template) {
        ReportCanvas canvas = new ReportCanvas("", ReportStyle.DEFAULT);
        SettlementDetailReportComposer.draw(canvas, report, template);
        return canvas.finish();
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

    private List<String> pageTextsOf(ReportDocument document, int pageIndex) {
        return document.getElements(pageIndex)
                .stream()
                .filter(ReportElement.Text.class::isInstance)
                .map(ReportElement.Text.class::cast)
                .map(ReportElement.Text::text)
                .toList();
    }

    @Nested
    @DisplayName("compose 테스트")
    class ComposeTest {

        @Test
        @DisplayName("문서 제목은 회계 연도의 이름을 넣은 회계 연도 결산 세부 내역서다")
        void compose_SettlementDetail_UsesFiscalYearNameInDocumentTitle() {
            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report(BudgetType.INCOME, 3),
                    report(BudgetType.EXPENSE, 3), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 결산 세부 내역서");
        }

        @Test
        @DisplayName("수입 결산 세부 내역서를 먼저 적고 지출 결산 세부 내역서는 새 페이지에서 시작한다")
        void compose_SettlementDetail_StartsExpenseDetailOnNewPage() {
            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report(BudgetType.INCOME, 3),
                    report(BudgetType.EXPENSE, 3), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(2);
            assertThat(pageTextsOf(document, 0)).contains("회계 연도 결산 세부 내역서 (수입)", "수입액")
                    .doesNotContain("회계 연도 결산 세부 내역서 (지출)");
            assertThat(pageTextsOf(document, 1)).contains("회계 연도 결산 세부 내역서 (지출)", "집행액")
                    .doesNotContain("수입액");
        }

        @Test
        @DisplayName("묶은 보고서의 수는 수입 내역서와 지출 내역서의 둘이다")
        void compose_SettlementDetail_HasTwoParts() {
            // When
            ReportDocument document = SettlementDetailReportComposer.compose(report(BudgetType.INCOME, 3),
                    report(BudgetType.EXPENSE, 3), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPartCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("draw 테스트")
    class DrawTest {

        @Test
        @DisplayName("수입 내역서는 제목에 구분을 괄호로 붙이고, 실제 금액을 수입액, 비율을 달성률로 부른다")
        void draw_IncomeReport_UsesIncomeTitleAndColumnNames() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(textsOf(document)).contains("회계 연도 결산 세부 내역서 (수입)", "수입액", "달성률");
        }

        @Test
        @DisplayName("지출 내역서는 제목에 구분을 괄호로 붙이고, 실제 금액을 집행액, 비율을 집행률로 부른다")
        void draw_ExpenseReport_UsesExpenseTitleAndColumnNames() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(textsOf(document)).contains("회계 연도 결산 세부 내역서 (지출)", "집행액", "집행률");
        }

        @Test
        @DisplayName("제목 아래에 회계 연도의 이름과 기간을 적는다")
        void draw_Subtitle_WritesFiscalYearNameAndPeriod() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(textsOf(document)).contains("회계 연도 : 2026년 (2025-12-01 ~ 2026-11-30)");
        }

        @Test
        @DisplayName("기본 양식에서는 예산 항목, 예산액, 점유율, 수입액, 달성률, 비고 순으로 적는다")
        void draw_DefaultTemplate_WritesColumnsInDefaultOrder() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.INCOME, 3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(List.of(xOf(document, "예산 항목"), xOf(document, "예산액"), xOf(document, "점유율"),
                    xOf(document, "수입액"), xOf(document, "달성률"), xOf(document, "비고"))).isSorted();
        }

        @Test
        @DisplayName("지출 내역서는 양식에서 정한 지출 결산 세부 내역 표의 칸 순서를 따른다")
        void draw_ExpenseTemplate_FollowsExpenseTableOrder() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.createTemplate(ReportType.SETTLEMENT_DETAIL,
                    Map.of(ReportTemplateTable.EXPENSE_SETTLEMENT_DETAIL, ReportTemplateFixtures.createColumns(
                            List.of(ReportColumn.BUDGET_NAME, ReportColumn.REMARKS, ReportColumn.BUDGETED_AMOUNT,
                                    ReportColumn.SHARE_RATE, ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE))),
                    true);

            // When
            ReportDocument document = drawAlone(report(BudgetType.EXPENSE, 3), template);

            // Then
            assertThat(xOf(document, "비고")).isLessThan(xOf(document, "예산액"));
        }

        @Test
        @DisplayName("칸에 들어가지 않는 비고는 예산 행 아래에 다시 적는다")
        void draw_LongRemarks_RewritesBelowRow() {
            // Given
            String remarks = "상반기에 행사가 취소되어 집행이 적었고 하반기에 수련회 비용을 한꺼번에 집행함 ".repeat(2)
                    .strip();
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 1, remarks);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(String.join("", textsOf(document))
                    .replace(" ", "")).contains(("비고: " + remarks).replace(" ", ""));
        }

        @Test
        @DisplayName("한 장에 담을 수 없을 만큼 예산이 많으면 다음 페이지로 넘긴다")
        void draw_TooManyBudgets_BreaksIntoMultiplePages() {
            // Given
            SettlementDetailReportDto report = report(BudgetType.EXPENSE, 200);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }
    }
}
