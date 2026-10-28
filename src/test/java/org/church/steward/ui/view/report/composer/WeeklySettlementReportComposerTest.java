package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WeeklySettlementReportComposerTest {

    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 4);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 10, 10);

    private ReportRecordDto record(String contactName, String description, String remarks) {
        return ReportRecordDto.builder()
                .date(WEEK_START)
                .contactNames(List.of(contactName))
                .description(description)
                .amount(50_000L)
                .remarks(remarks)
                .build();
    }

    private ReportRecordGroupDto group(String budgetName, List<ReportRecordDto> records) {
        return ReportRecordGroupDto.builder()
                .budgetName(budgetName)
                .totalAmount(records.size() * 50_000L)
                .records(records)
                .build();
    }

    /**
     * 예산마다 같은 수의 짧은 내역을 가진 묶음들을 만든다.
     */
    private List<ReportRecordGroupDto> groups(int groupCount, int recordCount) {
        List<ReportRecordGroupDto> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            List<ReportRecordDto> records = new ArrayList<>();
            for (int j = 0; j < recordCount; j++) {
                records.add(record("성도" + j, "헌금", ""));
            }
            groups.add(group("헌금 > 예산 " + i, records));
        }
        return groups;
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

    private WeeklySettlementReportDto report(List<ReportRecordGroupDto> incomeRecords,
            List<ReportRecordGroupDto> expenseRecords) {
        return WeeklySettlementReportDto.builder()
                .fiscalYearName("2026년")
                .startDate(WEEK_START)
                .endDate(WEEK_END)
                .previousBalance(12_345_000L)
                .weekly(ReportSummaryDto.builder()
                        .income(3_000_000L)
                        .expense(1_200_000L)
                        .build())
                .incomeStatus(status(6))
                .expenseStatus(status(10))
                .incomeRecords(incomeRecords)
                .expenseRecords(expenseRecords)
                .build();
    }

    private List<String> textsOf(ReportDocument document) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < document.getPageCount(); i++) {
            for (ReportElement element : document.getElements(i)) {
                if (element instanceof ReportElement.Text text) {
                    texts.add(text.text());
                }
            }
        }
        return texts;
    }

    @Nested
    @DisplayName("compose 테스트")
    class ComposeTest {

        @Test
        @DisplayName("파일 이름으로 쓸 제목에 회계 연도와 주간 범위를 넣는다")
        void compose_WeeklyReport_UsesYearAndPeriodInTitle() {
            // Given
            WeeklySettlementReportDto report = report(List.of(), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 주간 회계 결산 보고서 (2026-10-04 ~ 2026-10-10)");
        }

        @Test
        @DisplayName("개요에 전주 잔액과 주간 수입, 지출, 차액, 현재 잔액을 적는다")
        void compose_Overview_WritesBalancesAndWeeklyAmounts() {
            // Given
            WeeklySettlementReportDto report = report(List.of(), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("12,345,000", "3,000,000", "1,200,000", "1,800,000",
                    "14,145,000");
        }

        @Test
        @DisplayName("한 주에 수입 32건, 지출 20건 정도는 A4 한 장에 담는다")
        void compose_TypicalWeek_FitsOnSinglePage() {
            // Given
            WeeklySettlementReportDto report = report(groups(4, 8), groups(10, 2));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("내역이 한 장을 넘으면 다음 페이지의 같은 단으로 이어 적는다")
        void compose_TooManyRecords_BreaksIntoMultiplePages() {
            // Given
            WeeklySettlementReportDto report = report(groups(10, 30), groups(2, 3));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }

        @Test
        @DisplayName("칸에 다 들어가지 않는 내용은 말줄임표로 줄이고 아랫줄에 전체 내용을 다시 적는다")
        void compose_CellOverflow_ClipsCellAndRepeatsFullTextBelow() {
            // Given
            String longDescription = "교육관 냉난방기 교체 공사 잔금";
            WeeklySettlementReportDto report = report(List.of(),
                    List.of(group("관리비", List.of(record("OO설비", longDescription, "")))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            List<String> texts = textsOf(document);
            assertThat(texts).anyMatch(text -> text.endsWith("…") && longDescription.startsWith(
                    text.substring(0, text.length() - 1)));
            assertThat(texts).contains("내용: " + longDescription);
        }

        @Test
        @DisplayName("칸에 다 들어가는 내용은 줄이지 않고 아랫줄도 추가하지 않는다")
        void compose_CellFits_DoesNotAddContinuationLine() {
            // Given
            WeeklySettlementReportDto report = report(List.of(),
                    List.of(group("관리비", List.of(record("OO설비", "수리비", "")))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("수리비")
                    .noneMatch(text -> text.startsWith("내용: "));
        }
    }
}
