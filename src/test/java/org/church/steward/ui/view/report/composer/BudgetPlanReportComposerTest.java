package org.church.steward.ui.view.report.composer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetPlanReportComposerTest {

    private static final ReportTemplateDto DEFAULT_TEMPLATE = ReportTemplateFixtures.createDefaultTemplate(
            ReportType.BUDGET_PLAN);

    private BudgetPlanReportDto report(int rowCount) {
        return report(BudgetType.INCOME, rowCount, "예산 ", null);
    }

    private BudgetPlanReportDto report(int rowCount, String namePrefix, String remarks) {
        return report(BudgetType.INCOME, rowCount, namePrefix, remarks);
    }

    /**
     * @param namePrefix 예산명 앞부분. 뒤에 순번을 붙인다.
     * @param remarks    모든 예산의 비고. 앞에 "0번 "처럼 순번을 붙인다. null이면 비고가 없다.
     */
    private BudgetPlanReportDto report(BudgetType type, int rowCount, String namePrefix, String remarks) {
        List<BudgetPlanRowDto> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(BudgetPlanRowDto.builder()
                    .name(namePrefix + i)
                    .depth(i % 2)
                    .amount(1_000_000L)
                    .shareRate(1.0)
                    .previousAmount(i % 3 == 0 ? null : 900_000L)
                    .remarks(remarks == null ? null : i + "번 " + remarks)
                    .build());
        }
        return BudgetPlanReportDto.builder()
                .fiscalYearName("2026년")
                .fiscalYearStartDate(LocalDate.of(2026, 1, 1))
                .fiscalYearEndDate(LocalDate.of(2026, 12, 31))
                .type(type)
                .rows(rows)
                .totalAmount(rowCount * 1_000_000L)
                .build();
    }

    /**
     * 예산안 하나만 배치한 문서. 묶지 않고 배치해 그 예산안의 배치만 본다.
     */
    private ReportDocument drawAlone(BudgetPlanReportDto report, ReportTemplateDto template, ReportStyle style) {
        ReportCanvas canvas = new ReportCanvas("", style);
        BudgetPlanReportComposer.draw(canvas, report, template);
        return canvas.finish();
    }

    private List<String> pageTextsOf(ReportDocument document, int pageIndex) {
        return textElementsOf(document, pageIndex).stream()
                .map(ReportElement.Text::text)
                .toList();
    }

    private List<ReportElement.Text> textElementsOf(ReportDocument document, int pageIndex) {
        return document.getElements(pageIndex)
                .stream()
                .filter(ReportElement.Text.class::isInstance)
                .map(ReportElement.Text.class::cast)
                .toList();
    }

    private List<String> textsOf(ReportDocument document) {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < document.getPageCount(); i++) {
            for (ReportElement.Text text : textElementsOf(document, i)) {
                texts.add(text.text());
            }
        }
        return texts;
    }

    private ReportElement.Text find(ReportDocument document, String text) {
        return findOn(document, 0, text);
    }

    private ReportElement.Text findOn(ReportDocument document, int pageIndex, String text) {
        return textElementsOf(document, pageIndex).stream()
                .filter(element -> element.text()
                        .equals(text))
                .findFirst()
                .orElseThrow();
    }

    /**
     * 칸의 좌우 여백. 표 글자(9pt) 한 자의 폭이다.
     */
    private double padding() {
        return ReportText.width("가", ReportStyle.DEFAULT.regular(ReportStyles.BUDGET_TABLE_SIZE));
    }

    /**
     * 굵은 글자들 중 가장 넓은 것을 한 줄에 적는 칸의 폭(좌우 여백 포함). {@link #report(int)}로 만든 보고서는 머리글과
     * 굵게 적는 행(최상위 예산, 총계)의 글자가 가장 넓다.
     */
    private double boldColumnWidth(String... texts) {
        double width = 0;
        for (String text : texts) {
            width = Math.max(width, ReportText.width(text, ReportStyle.DEFAULT.bold(
                    ReportStyles.BUDGET_TABLE_SIZE)));
        }
        return width + padding() * 2;
    }

    private double rightOf(ReportElement.Text text) {
        return text.x() + ReportText.width(text.text(), text.font());
    }

    private double centerOf(ReportElement.Text text) {
        return text.x() + ReportText.width(text.text(), text.font()) / 2;
    }

    /**
     * 모든 글자를 공백 없이 잇는다. 여러 줄로 나눈 글자는 나눈 자리의 공백이 빠지므로 공백을 지우고 비교한다.
     */
    private String joinedWithoutSpaces(ReportDocument document) {
        return String.join("", textsOf(document))
                .replace(" ", "");
    }

    @Nested
    @DisplayName("compose 테스트")
    class ComposeTest {

        @Test
        @DisplayName("문서 제목은 회계 연도의 이름을 넣은 회계 연도 예산안이다")
        void compose_BudgetPlan_UsesFiscalYearNameInDocumentTitle() {
            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report(BudgetType.INCOME, 3, "수입 ", null),
                    report(BudgetType.EXPENSE, 3, "지출 ", null), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getTitle()).isEqualTo("2026년 회계 연도 예산안");
        }

        @Test
        @DisplayName("수입 예산안을 먼저 적고 지출 예산안은 새 페이지에서 시작한다")
        void compose_BudgetPlan_StartsExpensePlanOnNewPage() {
            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report(BudgetType.INCOME, 3, "수입 ", null),
                    report(BudgetType.EXPENSE, 3, "지출 ", null), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(2);
            assertThat(pageTextsOf(document, 0)).contains("회계 연도 예산안 (수입)", "수입 0")
                    .doesNotContain("회계 연도 예산안 (지출)", "지출 0");
            assertThat(pageTextsOf(document, 1)).contains("회계 연도 예산안 (지출)", "지출 0")
                    .doesNotContain("수입 0");
        }

        @Test
        @DisplayName("수입 예산안이 여러 페이지에 걸쳐도 지출 예산안은 그 다음 페이지에서 시작하고, 페이지 번호는 문서 전체로 센다")
        void compose_LongIncomePlan_StartsExpensePlanAfterLastIncomePage() {
            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report(BudgetType.INCOME, 200, "수입 ", null),
                    report(BudgetType.EXPENSE, 3, "지출 ", null), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            int last = document.getPageCount() - 1;
            assertThat(pageTextsOf(document, last)).contains("회계 연도 예산안 (지출)",
                    document.getPageCount() + " / " + document.getPageCount());
            assertThat(pageTextsOf(document, last - 1)).doesNotContain("지출 0");
        }

        @Test
        @DisplayName("묶은 보고서의 수는 수입 예산안과 지출 예산안의 둘이다")
        void compose_BudgetPlan_HasTwoParts() {
            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report(BudgetType.INCOME, 3, "수입 ", null),
                    report(BudgetType.EXPENSE, 3, "지출 ", null), DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPartCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("지출 예산안은 양식에서 정한 지출 예산안 표의 칸 순서를 따른다")
        void compose_ExpenseTemplate_AppliesOnlyToExpensePlan() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.createTemplate(ReportType.BUDGET_PLAN,
                    Map.of(ReportTemplateTable.EXPENSE_BUDGET_PLAN, ReportTemplateFixtures.createColumns(List.of(
                            ReportColumn.BUDGET_NAME, ReportColumn.REMARKS, ReportColumn.BUDGETED_AMOUNT,
                            ReportColumn.SHARE_RATE, ReportColumn.CHANGE))),
                    true);

            // When
            ReportDocument document = BudgetPlanReportComposer.compose(report(BudgetType.INCOME, 3, "수입 ", null),
                    report(BudgetType.EXPENSE, 3, "지출 ", null), template, ReportStyle.DEFAULT);

            // Then
            assertThat(findOn(document, 0, "비고").x()).isGreaterThan(findOn(document, 0, "예산액").x());
            assertThat(findOn(document, 1, "비고").x()).isLessThan(findOn(document, 1, "예산액").x());
        }
    }

    @Nested
    @DisplayName("draw 테스트")
    class DrawTest {

        @Test
        @DisplayName("제목은 회계 연도의 이름 없이 구분을 괄호로 붙인다")
        void draw_Title_AppendsTypeWithoutFiscalYearName() {
            // Given
            BudgetPlanReportDto report = report(3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("회계 연도 예산안 (수입)")
                    .noneMatch(text -> text.startsWith("2026년 회계 연도"));
        }

        @Test
        @DisplayName("제목 아래에 회계 연도의 이름과 기간을 적는다")
        void draw_Subtitle_WritesFiscalYearNameAndPeriod() {
            // Given
            BudgetPlanReportDto report = report(3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            ReportElement.Text title = find(document, "회계 연도 예산안 (수입)");
            ReportElement.Text subtitle = find(document, "회계 연도 : 2026년 (2026-01-01 ~ 2026-12-31)");
            assertThat(subtitle.baseline()).isGreaterThan(title.baseline());
        }

        @Test
        @DisplayName("소규모 교회 수준의 예산 수(30개)는 A4 한 장에 담는다")
        void draw_ThirtyBudgets_FitsOnSinglePage() {
            // Given
            BudgetPlanReportDto report = report(30);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("한 장에 담을 수 없을 만큼 예산이 많으면 다음 페이지로 넘긴다")
        void draw_TooManyBudgets_BreaksIntoMultiplePages() {
            // Given
            BudgetPlanReportDto report = report(200);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
        }

        @Test
        @DisplayName("예산이 하나도 없어도 머리글과 총계만 있는 한 페이지를 만든다")
        void draw_NoBudgets_ReturnsSinglePage() {
            // Given
            BudgetPlanReportDto report = report(0);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("가장 작은 글자는 금액 단위(8pt)이며 글자 크기를 조정하면 함께 바뀐다")
        void draw_FontSizeStep_ChangesSmallestFontSize() {
            // Given
            BudgetPlanReportDto report = report(3);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT.withFontSizeStep(2));

            // Then
            assertThat(document.getSmallestFontSize()).isEqualTo(9f);
            assertThat(document.getMaxFontSizeStep()).isEqualTo(8);
        }

        @Test
        @DisplayName("행 간격을 늘리면 같은 예산이 더 많은 페이지를 차지한다")
        void draw_LargerLineSpacing_TakesMorePages() {
            // Given
            BudgetPlanReportDto report = report(120);

            // When
            ReportDocument tight = drawAlone(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT.withLineSpacing(100));
            ReportDocument loose = drawAlone(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT.withLineSpacing(200));

            // Then
            assertThat(loose.getPageCount()).isGreaterThan(tight.getPageCount());
        }
    }

    @Nested
    @DisplayName("draw 테스트 - 칸")
    class DrawColumnTest {

        @Test
        @DisplayName("기본 양식에서는 예산 항목, 예산액, 점유율, 직전 대비 증감액, 비고 순으로 적는다")
        void draw_DefaultTemplate_WritesColumnsInDefaultOrder() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(List.of(find(document, "예산 항목").x(), find(document, "예산액").x(),
                    find(document, "점유율").x(), find(document, "직전 대비 증감액").x(), find(document, "비고").x()))
                    .isSorted();
        }

        @Test
        @DisplayName("양식에서 바꾼 칸 순서대로 적는다")
        void draw_ReorderedTemplate_FollowsTemplateOrder() {
            // Given
            ReportTemplateDto template = ReportTemplateFixtures.createTemplate(ReportType.BUDGET_PLAN,
                    Map.of(ReportTemplateTable.INCOME_BUDGET_PLAN, ReportTemplateFixtures.createColumns(List.of(
                            ReportColumn.BUDGET_NAME, ReportColumn.REMARKS, ReportColumn.BUDGETED_AMOUNT,
                            ReportColumn.SHARE_RATE, ReportColumn.CHANGE))),
                    true);

            // When
            ReportDocument document = drawAlone(report(3), template, ReportStyle.DEFAULT);

            // Then
            assertThat(find(document, "비고").x()).isLessThan(find(document, "예산액").x());
        }

        @Test
        @DisplayName("남는 폭은 모든 칸이 똑같이 나눠 가지며, 예산명이 짧으면 예산 항목은 최소 폭(표 폭의 1/5)에서 시작한다")
        void draw_RemainingWidth_SplitsEvenly() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            double tableWidth = ReportPaper.WIDTH - ReportPaper.MARGIN * 2;
            double name = tableWidth / 5;
            double amount = boldColumnWidth("예산액", "1,000,000", "3,000,000");
            double share = boldColumnWidth("점유율", "1.0%", "100.0%");
            double change = boldColumnWidth("직전 대비 증감액", "+100,000", "신규");
            double remarks = boldColumnWidth("비고");
            double extra = (tableWidth - name - amount - share - change - remarks) / 5;
            assertThat(centerOf(find(document, "예산액"))).isCloseTo(ReportPaper.MARGIN + name + extra
                    + (amount + extra) / 2, within(0.01));
        }

        @Test
        @DisplayName("숫자 칸의 숫자들은 서로 오른쪽 끝을 맞춘다")
        void draw_Amounts_RightAlignedTogether() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(rightOf(find(document, "1,000,000"))).isCloseTo(rightOf(find(document, "3,000,000")),
                    within(0.01));
        }

        @Test
        @DisplayName("칸이 넓어져도 숫자들은 칸의 오른쪽 끝에 붙지 않고, 가장 넓은 금액(총계)이 머리글과 같은 가운데에 놓이도록 덩어리로 가운데에 둔다")
        void draw_WidestAmount_CenteredUnderHeader() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(centerOf(find(document, "3,000,000"))).isCloseTo(centerOf(find(document, "예산액")),
                    within(0.01));
        }

        @Test
        @DisplayName("직전 대비 증감액 칸에 신규를 적을 때는 칸의 가운데에 적는다")
        void draw_NewBudget_CentersChangeText() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(centerOf(find(document, "신규"))).isCloseTo(centerOf(find(document, "직전 대비 증감액")),
                    within(0.01));
        }

        @Test
        @DisplayName("직전 대비 증감액 칸의 금액은 신규를 뺀 가장 넓은 증감액을 머리글 가운데에 둔 덩어리의 오른쪽 끝에 맞춘다")
        void draw_PreviousAmount_RightAlignsChangeWithinBlock() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            // 가장 넓은 증감액은 최상위 예산 행에 굵게 적은 "+100,000"이다.
            double blockWidth = ReportText.width("+100,000", ReportStyle.DEFAULT.bold(ReportStyles.BUDGET_TABLE_SIZE));
            assertThat(rightOf(find(document, "+100,000"))).isCloseTo(centerOf(find(document, "직전 대비 증감액"))
                    + blockWidth / 2, within(0.01));
        }

        @Test
        @DisplayName("비고는 가장 넓은 비고가 머리글과 같은 가운데에 오도록 덩어리로 칸 가운데에 두고 서로 왼쪽 끝을 맞춘다")
        void draw_ShortRemarks_CentersRemarksBlockUnderHeader() {
            // Given
            BudgetPlanReportDto report = report(3, "예산 ", "확인");

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            List<ReportElement.Text> remarks = List.of(find(document, "0번 확인"), find(document, "1번 확인"),
                    find(document, "2번 확인"));
            ReportElement.Text widest = remarks.stream()
                    .max((a, b) -> Double.compare(ReportText.width(a.text(), a.font()), ReportText.width(b.text(),
                            b.font())))
                    .orElseThrow();
            assertThat(centerOf(widest)).isCloseTo(centerOf(find(document, "비고")), within(0.01));
            assertThat(remarks).allSatisfy(text -> assertThat(text.x()).isCloseTo(widest.x(), within(0.01)));
        }

        @Test
        @DisplayName("칸에 들어가지 않는 비고는 말줄임표로 줄이고 예산 행 아래에 전체 내용을 다시 적는다")
        void draw_LongRemarks_ClipsAndRewritesBelowRow() {
            // Given
            String remarks = "작년보다 성도 수가 늘어 십일조 예상액을 조정했으며 상반기 결산 후 다시 검토하기로 함 ".repeat(2)
                    .strip();
            BudgetPlanReportDto report = report(1, "예산 ", remarks);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            String fullRemarks = "0번 " + remarks;
            assertThat(textsOf(document)).anyMatch(text -> text.endsWith("…") && fullRemarks.startsWith(text
                    .substring(0, text.length() - 1)));
            assertThat(joinedWithoutSpaces(document)).contains(("비고: " + fullRemarks).replace(" ", ""));
        }

        @Test
        @DisplayName("예산 행과 그 아래에 다시 적는 비고는 페이지 사이에서 갈라지지 않는다")
        void draw_LongRemarksAcrossPages_KeepsRowAndRemarksTogether() {
            // Given
            BudgetPlanReportDto report = report(120, "예산 ",
                    "작년보다 성도 수가 늘어 십일조 예상액을 조정했으며 상반기 결산 후 다시 검토하기로 한 예산 ");

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            assertThat(document.getPageCount()).isGreaterThan(1);
            for (int page = 0; page < document.getPageCount(); page++) {
                List<String> texts = textElementsOf(document, page).stream()
                        .map(ReportElement.Text::text)
                        .toList();
                for (String text : texts) {
                    if (text.startsWith("비고: ")) {
                        // 다시 적는 첫 줄은 "비고: 7번 …"이고, 그 예산의 이름은 "예산 7"이다.
                        String number = text.substring("비고: ".length())
                                .split("번")[0];
                        assertThat(texts).as("%d쪽의 %s", page + 1, text)
                                .contains("예산 " + number);
                    }
                }
            }
        }

        @Test
        @DisplayName("예산명이 아주 길면 비고를 머리글 폭까지 줄이고 예산 항목을 여러 줄로 감싼다")
        void draw_VeryLongNames_ShrinksRemarksAndWrapsName() {
            // Given
            String longName = "아주 긴 예산명 ".repeat(20);
            BudgetPlanReportDto report = report(1, longName, "비고");

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE, ReportStyle.DEFAULT);

            // Then
            List<String> texts = textsOf(document);
            assertThat(texts).doesNotContain(longName + 0)
                    .anyMatch(text -> text.startsWith("아주 긴 예산명") && text.length() < longName.length());
            assertThat(texts).contains("1,000,000", "비고");
            ReportElement.Text remarksHeader = find(document, "비고");
            assertThat(remarksHeader.x() + ReportText.width(remarksHeader.text(), remarksHeader.font()))
                    .isLessThanOrEqualTo(ReportPaper.WIDTH - ReportPaper.MARGIN);
        }

        @Test
        @DisplayName("글자 크기를 키워도 금액은 줄을 바꾸지 않고 긴 비고는 아랫줄에 모두 적는다")
        void draw_LargerFont_KeepsAmountsAndRewritesRemarks() {
            // Given
            String remarks = "작년보다 성도 수가 늘어 십일조 예상액을 조정했으며 상반기 결산 후 다시 검토하기로 함";
            BudgetPlanReportDto report = report(3, "예산 ", remarks);

            // When
            ReportDocument document = drawAlone(report, DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT.withFontSizeStep(8));

            // Then
            assertThat(textsOf(document)).contains("1,000,000", "3,000,000");
            assertThat(joinedWithoutSpaces(document)).contains(("비고: 0번 " + remarks).replace(" ", ""));
        }

        @Test
        @DisplayName("직전 회계 연도에 없던 예산은 증감액 대신 신규로 적는다")
        void draw_NoPreviousAmount_WritesNew() {
            // When
            ReportDocument document = drawAlone(report(3), DEFAULT_TEMPLATE,
                    ReportStyle.DEFAULT);

            // Then
            assertThat(textsOf(document)).contains("신규", "+100,000");
        }
    }
}
