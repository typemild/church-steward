package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;
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
        return report("2026년", incomeRecords, expenseRecords);
    }

    private WeeklySettlementReportDto report(String fiscalYearName, List<ReportRecordGroupDto> incomeRecords,
            List<ReportRecordGroupDto> expenseRecords) {
        return WeeklySettlementReportDto.builder()
                .fiscalYearName(fiscalYearName)
                .fiscalYearStartDate(LocalDate.of(2026, 1, 1))
                .fiscalYearEndDate(LocalDate.of(2026, 12, 31))
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

    private static ReportApprovalDto createApproval(boolean visible, String... titles) {
        return ReportApprovalDto.builder()
                .visible(visible)
                .titles(Arrays.asList(titles))
                .build();
    }

    /**
     * 칸들을 순서대로 담는다.
     *
     * @param hidden 숨길 칸
     */
    private static List<ReportColumnDto> createColumns(List<ReportColumn> columns, ReportColumn... hidden) {
        Set<ReportColumn> hiddenSet = Set.of(hidden);
        return columns.stream()
                .map(column -> ReportColumnDto.builder()
                        .column(column)
                        .hidden(hiddenSet.contains(column))
                        .build())
                .toList();
    }

    /**
     * 기본 양식(결재란 세 칸, 모든 표가 기본 순서)
     */
    private static ReportTemplateDto createTemplate() {
        return createTemplate(createApproval(true, null, null, null), Map.of());
    }

    /**
     * @param overrides 기본 순서 대신 쓸 표의 칸
     */
    private static ReportTemplateDto createTemplate(ReportApprovalDto approval,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides) {
        return createTemplate(approval, overrides, true);
    }

    /**
     * @param overrides      기본 순서 대신 쓸 표의 칸
     * @param rateBarVisible 모든 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부
     */
    private static ReportTemplateDto createTemplate(ReportApprovalDto approval,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides, boolean rateBarVisible) {
        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBars = new EnumMap<>(ReportTemplateTable.class);
        for (ReportTemplateTable table : ReportTemplateTable.of(ReportType.WEEKLY_SETTLEMENT)) {
            tables.put(table, overrides.getOrDefault(table, createColumns(table.getDefaultColumns())));
            if (table.hasRateBarOption()) {
                rateBars.put(table, rateBarVisible);
            }
        }
        return ReportTemplateDto.builder()
                .reportType(ReportType.WEEKLY_SETTLEMENT)
                .approval(approval)
                .tables(tables)
                .rateBarVisible(rateBars)
                .build();
    }

    private List<ReportElement.Text> textElementsOf(ReportDocument document) {
        List<ReportElement.Text> texts = new ArrayList<>();
        for (int i = 0; i < document.getPageCount(); i++) {
            for (ReportElement element : document.getElements(i)) {
                if (element instanceof ReportElement.Text text) {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    private List<String> textsOf(ReportDocument document) {
        return textElementsOf(document).stream()
                .map(ReportElement.Text::text)
                .toList();
    }

    private List<ReportElement.Rect> rectsOf(ReportDocument document) {
        List<ReportElement.Rect> rects = new ArrayList<>();
        for (int i = 0; i < document.getPageCount(); i++) {
            for (ReportElement element : document.getElements(i)) {
                if (element instanceof ReportElement.Rect rect) {
                    rects.add(rect);
                }
            }
        }
        return rects;
    }

    /**
     * 글자가 처음 나오는 요소. 같은 글자가 여러 번 나오면 가장 왼쪽의 것이다.
     */
    private ReportElement.Text findLeftmost(ReportDocument document, String text) {
        return textElementsOf(document).stream()
                .filter(element -> element.text()
                        .equals(text))
                .min((a, b) -> Double.compare(a.x(), b.x()))
                .orElseThrow();
    }

    private long countFills(ReportDocument document, Color color) {
        return rectsOf(document).stream()
                .filter(rect -> color.equals(rect.fill()))
                .count();
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
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 주간 회계 결산 보고서 (2026-10-04 ~ 2026-10-10)");
        }

        @Test
        @DisplayName("머리 부분에 회계 연도와 그 기간, 결산 기간을 적는다")
        void compose_Header_WritesFiscalYearPeriodAndSettlementPeriod() {
            // Given
            WeeklySettlementReportDto report = report(List.of(), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("회계 연도 : ", "2026년 (2026-01-01 ~ 2026-12-31)", "결산 기간 : ");
        }

        @Test
        @DisplayName("개요에 전주 잔액과 주간 수입, 지출, 차액, 현재 잔액을 적는다")
        void compose_Overview_WritesBalancesAndWeeklyAmounts() {
            // Given
            WeeklySettlementReportDto report = report(List.of(), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

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
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("내역이 한 장을 넘으면 다음 페이지의 같은 단으로 이어 적는다")
        void compose_TooManyRecords_BreaksIntoMultiplePages() {
            // Given
            WeeklySettlementReportDto report = report(groups(10, 30), groups(2, 3));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("compose 테스트 - 결재란")
    class ComposeApprovalTest {

        @Test
        @DisplayName("결재란의 칸마다 직함을 적고 빈 직함은 비워 둔다")
        void compose_ApprovalTitles_WritesEachTitle() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, "담당", null, "담임목사"), Map.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("결", "재", "담당", "담임목사");
        }

        @Test
        @DisplayName("결재란을 숨기면 결재란을 그리지 않는다")
        void compose_ApprovalHidden_DoesNotDrawApproval() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(false, "담당", "부장", "담임목사"), Map.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).doesNotContain("결", "재", "담당");
        }

        @Test
        @DisplayName("칸에 들어가지 않는 직함은 말줄임표 없이 글자를 작게 줄여 모두 적는다")
        void compose_LongApprovalTitle_ShrinksFontWithoutEllipsis() {
            // Given
            String title = "재정부위원장님";
            ReportTemplateDto template = createTemplate(createApproval(true, title), Map.of());
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(4);

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    style);

            // Then
            ReportElement.Text text = findLeftmost(document, title);
            assertThat(text.font()
                    .getSize2D()).isLessThan(style.size(ReportFonts.SIZE_BODY));
        }

        @Test
        @DisplayName("작게 줄인 직함은 글자 크기를 조정할 수 있는 범위를 좁히지 않는다")
        void compose_LongApprovalTitle_DoesNotChangeSmallestFontSize() {
            // Given
            ReportTemplateDto longTitle = createTemplate(createApproval(true, "재정부위원장님"), Map.of());
            ReportTemplateDto noTitle = createTemplate(createApproval(true, (String) null), Map.of());
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(4);

            // When
            ReportDocument withLongTitle = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    longTitle, style);
            ReportDocument withoutTitle = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    noTitle, style);

            // Then
            assertThat(withLongTitle.getSmallestFontSize()).isEqualTo(withoutTitle.getSmallestFontSize());
        }

        @Test
        @DisplayName("결재란이 다섯 칸이고 회계 연도 이름이 길어도 글자를 키운 머리 부분의 글자가 결재란을 침범하지 않는다")
        void compose_FiveApprovalCellsWithLongName_KeepsHeaderTextLeftOfApproval() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, "담당", "회계", "재정부장", "장로", "담임목사"),
                    Map.of());
            WeeklySettlementReportDto report = report("2026년 상반기 특별 회계 연도 이름은 서른 글자까지", List.of(),
                    List.of());
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(6);
            double approvalLeft = ReportPaper.WIDTH - ReportPaper.MARGIN - ReportPaper.mm(6) - ReportPaper.mm(20) * 5;

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, template, style);

            // Then
            double unitBaseline = findLeftmost(document, "(단위: 원)").baseline();
            assertThat(textElementsOf(document)).filteredOn(text -> text.baseline() < unitBaseline
                    && text.x() < approvalLeft)
                    .isNotEmpty()
                    .allSatisfy(text -> assertThat(text.x() + ReportText.width(text.text(), text.font()))
                            .isLessThanOrEqualTo(approvalLeft));
        }
    }

    @Nested
    @DisplayName("compose 테스트 - 수입 현황/지출 현황")
    class ComposeStatusTest {

        @Test
        @DisplayName("기본 양식에서는 예산 항목, 수입액, 예산액, 달성률 순으로 적는다")
        void compose_DefaultStatus_WritesActualBeforeBudgeted() {
            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    createTemplate(), ReportStyle.DEFAULT);

            // Then
            assertThat(findLeftmost(document, "수입액").x()).isLessThan(findLeftmost(document, "예산액").x());
        }

        @Test
        @DisplayName("양식에서 바꾼 칸 순서대로 적는다")
        void compose_ReorderedStatus_FollowsTemplateOrder() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null),
                    Map.of(ReportTemplateTable.WEEKLY_INCOME_STATUS, createColumns(List.of(ReportColumn.BUDGET_NAME,
                            ReportColumn.BUDGETED_AMOUNT, ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(findLeftmost(document, "예산액").x()).isLessThan(findLeftmost(document, "수입액").x());
        }

        @Test
        @DisplayName("막대를 숨기면 막대를 그리지 않는다")
        void compose_BarHidden_DoesNotDrawBars() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null), Map.of(), false);

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(rectsOf(document)).noneMatch(rect -> UIColors.REPORT_BAR.equals(rect.stroke()));
        }

        @Test
        @DisplayName("막대를 보이면 달성률 칸 안에서 비율의 왼쪽에 막대를 그린다")
        void compose_BarVisible_DrawsBarLeftOfRateInRateColumn() {
            // Given
            double tableRight = ReportPaper.MARGIN + (ReportPaper.WIDTH - ReportPaper.MARGIN * 2
                    - BudgetStatusSection.COLUMN_GAP) / 2;

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    createTemplate(), ReportStyle.DEFAULT);

            // Then
            ReportElement.Text rate = findLeftmost(document, "40.0%");
            ReportElement.Text budgeted = findLeftmost(document, "10,000,000");
            double budgetedRight = budgeted.x() + ReportText.width(budgeted.text(), budgeted.font());
            assertThat(rectsOf(document)).filteredOn(rect -> UIColors.REPORT_BAR.equals(rect.stroke())
                    && rect.x() < tableRight)
                    .isNotEmpty()
                    .allSatisfy(rect -> {
                        assertThat(rect.x()).isGreaterThan(budgetedRight);
                        assertThat(rect.x() + rect.width()).isLessThan(rate.x());
                    });
        }

        @Test
        @DisplayName("막대가 없으면 달성률을 칸의 가운데에 적는다")
        void compose_BarHidden_CentersRateInColumn() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null), Map.of(), false);
            double tableWidth = (ReportPaper.WIDTH - ReportPaper.MARGIN * 2 - BudgetStatusSection.COLUMN_GAP) / 2;

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()), template,
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text header = findLeftmost(document, "달성률");
            ReportElement.Text rate = findLeftmost(document, "40.0%");
            double headerCenter = header.x() + ReportText.width(header.text(), header.font()) / 2;
            double rateCenter = rate.x() + ReportText.width(rate.text(), rate.font()) / 2;
            assertThat(rateCenter).isCloseTo(headerCenter, within(0.01));
            assertThat(headerCenter).isCloseTo(ReportPaper.MARGIN + tableWidth * 7 / 8, within(0.01));
        }

        @Test
        @DisplayName("모든 칸이 표의 폭을 고르게 나눠 갖는다")
        void compose_Status_SplitsWidthEvenly() {
            // Given
            double tableWidth = (ReportPaper.WIDTH - ReportPaper.MARGIN * 2 - BudgetStatusSection.COLUMN_GAP) / 2;

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    createTemplate(), ReportStyle.DEFAULT);

            // Then
            ReportElement.Text header = findLeftmost(document, "수입액");
            double headerCenter = header.x() + ReportText.width(header.text(), header.font()) / 2;
            assertThat(headerCenter).isCloseTo(ReportPaper.MARGIN + tableWidth * 3 / 8, within(0.01));
        }
    }

    @Nested
    @DisplayName("compose 테스트 - 주간 내역")
    class ComposeRecordTest {

        @Test
        @DisplayName("절 제목을 주간 수입 내역과 주간 지출 내역으로 적는다")
        void compose_Records_UsesWeeklySectionTitles() {
            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report(List.of(), List.of()),
                    createTemplate(), ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("주간 수입 내역", "주간 지출 내역");
        }

        @Test
        @DisplayName("일자는 첫 번째 칸의 머리글과 내용 앞에 붙여 적는다")
        void compose_Records_PrefixesDateToFirstColumn() {
            // Given
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "", "")))),
                    List.of(group("관리비", List.of(record("OO설비", "수리비", "")))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("일자 · 봉헌자", "일자 · 내용", "[10/4] 홍길동", "[10/4] 수리비");
        }

        @Test
        @DisplayName("첫 번째 칸이 아닌 관계자는 머리글과 같이 칸의 가운데에 적는다")
        void compose_ContactNotFirst_CentersContactInColumn() {
            // Given
            WeeklySettlementReportDto report = report(List.of(),
                    List.of(group("관리비", List.of(record("OO설비", "수리비", "")))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text header = findLeftmost(document, "수취인");
            ReportElement.Text contact = findLeftmost(document, "OO설비");
            double headerCenter = header.x() + ReportText.width(header.text(), header.font()) / 2;
            double contactCenter = contact.x() + ReportText.width(contact.text(), contact.font()) / 2;
            assertThat(contactCenter).isCloseTo(headerCenter, within(0.01));
        }

        @Test
        @DisplayName("기본 양식에서는 수입 내역의 금액이 봉헌자 바로 옆에 붙는다")
        void compose_DefaultIncomeRecords_PutsAmountNextToContact() {
            // Given
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "", "")))),
                    List.of());
            double columnWidth = (ReportPaper.WIDTH - ReportPaper.MARGIN * 2 - BudgetStatusSection.COLUMN_GAP) / 2;

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text amount = findLeftmost(document, "50,000");
            assertThat(amount.x() + ReportText.width(amount.text(), amount.font()))
                    .isLessThan(ReportPaper.MARGIN + columnWidth / 2);
        }

        @Test
        @DisplayName("금액 뒤의 칸은 금액과 한 글자 폭 이상 떨어뜨려 적는다")
        void compose_ColumnAfterAmount_LeavesGapAfterAmount() {
            // Given
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "감사헌금",
                    "")))), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            ReportElement.Text amount = findLeftmost(document, "50,000");
            ReportElement.Text description = findLeftmost(document, "감사헌금");
            double amountRight = amount.x() + ReportText.width(amount.text(), amount.font());
            assertThat(description.x() - amountRight).isGreaterThanOrEqualTo(ReportText.width("가", description
                    .font()));
        }

        @Test
        @DisplayName("금액 뒤의 칸들은 남은 폭을 내용의 길이에 비례해 나눠 가져, 내용이 긴 중간 칸도 줄이지 않는다")
        void compose_ColumnsAfterAmount_ShareRemainingWidthByContent() {
            // Given
            String description = "새가족 등록 기념 감사 헌금";
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", description,
                    "확인")))), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains(description)
                    .noneMatch(text -> text.startsWith("내용: "));
        }

        @Test
        @DisplayName("칸에 다 들어가지 않는 내용은 말줄임표로 줄이고 아랫줄에 전체 내용을 다시 적는다")
        void compose_CellOverflow_ClipsCellAndRepeatsFullTextBelow() {
            // Given
            String longRemarks = "교육관 냉난방기 교체 공사의 잔금으로 계약서에 따라 지급함";
            WeeklySettlementReportDto report = report(List.of(),
                    List.of(group("관리비", List.of(record("OO설비", "수리비", longRemarks)))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            List<String> texts = textsOf(document);
            assertThat(texts).anyMatch(text -> text.endsWith("…") && longRemarks.startsWith(
                    text.substring(0, text.length() - 1)));
            assertThat(texts).contains("비고: " + longRemarks);
        }

        @Test
        @DisplayName("칸에 다 들어가는 내용은 줄이지 않고 아랫줄도 추가하지 않는다")
        void compose_CellFits_DoesNotAddContinuationLine() {
            // Given
            WeeklySettlementReportDto report = report(List.of(),
                    List.of(group("관리비", List.of(record("OO설비", "수리비", "")))));

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("[10/4] 수리비")
                    .noneMatch(text -> text.startsWith("내용: "));
        }

        @Test
        @DisplayName("숨긴 칸에 값이 있으면 칸 이름을 붙여 아랫줄에 적는다")
        void compose_HiddenColumnWithValue_WritesValueBelow() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null),
                    Map.of(ReportTemplateTable.WEEKLY_INCOME_RECORDS, createColumns(
                            ReportTemplateTable.WEEKLY_INCOME_RECORDS.getDefaultColumns(), ReportColumn.DESCRIPTION)));
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "감사헌금",
                    "")))), List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, template, ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("내용: 감사헌금")
                    .doesNotContain("감사헌금");
        }

        @Test
        @DisplayName("숨긴 칸에 값이 없으면 아랫줄을 추가하지 않는다")
        void compose_HiddenColumnWithoutValue_DoesNotAddLine() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null),
                    Map.of(ReportTemplateTable.WEEKLY_INCOME_RECORDS, createColumns(
                            ReportTemplateTable.WEEKLY_INCOME_RECORDS.getDefaultColumns(), ReportColumn.REMARKS)));
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "", "")))),
                    List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, template, ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).noneMatch(text -> text.startsWith("비고: "));
        }

        @Test
        @DisplayName("예산 행에는 줄무늬보다 짙은 바탕을 깐다")
        void compose_BudgetRow_UsesStrongFill() {
            // Given
            WeeklySettlementReportDto report = report(List.of(group("주일헌금", List.of(record("홍길동", "", "")))),
                    List.of());

            // When
            ReportDocument document = WeeklySettlementReportComposer.compose(report, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(countFills(document, UIColors.REPORT_FILL_STRONG)).isEqualTo(1);
        }

        @Test
        @DisplayName("예산 아래 첫 내역은 흰 바탕이고 두 번째 내역부터 한 건씩 번갈아 바탕을 깐다")
        void compose_Records_StripesFromSecondRecord() {
            // Given
            WeeklySettlementReportDto oneRecord = report(List.of(group("주일헌금", List.of(record("홍길동", "", "")))),
                    List.of());
            WeeklySettlementReportDto twoRecords = report(List.of(group("주일헌금", List.of(record("홍길동", "", ""),
                    record("김철수", "", "")))), List.of());

            // When
            ReportDocument withOne = WeeklySettlementReportComposer.compose(oneRecord, createTemplate(),
                    ReportStyle.DEFAULT);
            ReportDocument withTwo = WeeklySettlementReportComposer.compose(twoRecords, createTemplate(),
                    ReportStyle.DEFAULT);

            // Then
            assertThat(countFills(withTwo, UIColors.REPORT_FILL) - countFills(withOne, UIColors.REPORT_FILL))
                    .isEqualTo(1);
        }
    }
}
