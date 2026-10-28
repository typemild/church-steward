package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetPlanReportComposerTest {

    private BudgetPlanReportDto report(int rowCount) {
        List<BudgetPlanRowDto> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(BudgetPlanRowDto.builder()
                    .name("예산 " + i)
                    .depth(i % 2)
                    .amount(1_000_000L)
                    .shareRate(1.0)
                    .previousAmount(i % 3 == 0 ? null : 900_000L)
                    .build());
        }
        return BudgetPlanReportDto.builder()
                .fiscalYearName("2026년")
                .type(BudgetType.INCOME)
                .rows(rows)
                .totalAmount(rowCount * 1_000_000L)
                .build();
    }

    @Nested
    @DisplayName("compose 테스트")
    class ComposeTest {

        @Test
        @DisplayName("회계 연도와 구분을 넣은 제목을 붙인다")
        void compose_IncomeBudgetPlan_UsesYearAndTypeInTitle() {
            // Given
            BudgetPlanReportDto report = report(3);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 수입 예산안");
        }

        @Test
        @DisplayName("소규모 교회 수준의 예산 수(30개)는 A4 한 장에 담는다")
        void compose_ThirtyBudgets_FitsOnSinglePage() {
            // Given
            BudgetPlanReportDto report = report(30);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("한 장에 담을 수 없을 만큼 예산이 많으면 다음 페이지로 넘긴다")
        void compose_TooManyBudgets_BreaksIntoMultiplePages() {
            // Given
            BudgetPlanReportDto report = report(200);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }

        @Test
        @DisplayName("예산이 하나도 없어도 머리글과 총계만 있는 한 페이지를 만든다")
        void compose_NoBudgets_ReturnsSinglePage() {
            // Given
            BudgetPlanReportDto report = report(0);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("가장 작은 글자는 금액 단위(8pt)이며 글자 크기를 조정하면 함께 바뀐다")
        void compose_FontSizeStep_ChangesSmallestFontSize() {
            // Given
            BudgetPlanReportDto report = report(3);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT.withFontSizeStep(2));

            // Then
            assertThat(document.getSmallestFontSize()).isEqualTo(9f);
            assertThat(document.getMaxFontSizeStep()).isEqualTo(8);
        }

        @Test
        @DisplayName("행 간격을 늘리면 같은 예산이 더 많은 페이지를 차지한다")
        void compose_LargerLineSpacing_TakesMorePages() {
            // Given
            BudgetPlanReportDto report = report(120);

            // When
            ReportDocument tight = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT.withLineSpacing(100));
            ReportDocument loose = BudgetPlanReportComposer.compose(report, ReportStyle.DEFAULT.withLineSpacing(200));

            // Then
            assertThat(loose.getPageCount()).isGreaterThan(tight.getPageCount());
        }
    }
}
