package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateConstraints;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Cell;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 수입 현황과 지출 현황. 최상위 예산별로 예산액 대비 실제 금액을 막대와 숫자로 함께 보여준다. 대시보드의 "예산 집행 현황"과 같은
 * 내용이지만, 종이에서는 툴팁을 볼 수 없으므로 금액과 비율을 모두 적는다. 두 현황은 좌우로 나란히 놓는다.
 * <p>
 * 칸의 순서와 비율 칸(달성률/집행률)의 막대는 보고서 양식이 정한다. 막대는 따로 칸을 두지 않고 비율 칸 안의 왼쪽에 그리며, 비율은
 * 가운데 정렬한다. 모든 칸이 폭을 고르게 나눠 갖는다(글자를 키워 금액이나 비율이 들어가지 않는 칸은 넓어지고 예산 항목이 그만큼
 * 좁아진다).
 */
final class BudgetStatusSection {

    /** 수입 현황과 지출 현황 사이의 간격 */
    static final double COLUMN_GAP = ReportPaper.mm(4);

    /**
     * 현황 표 하나의 양식
     *
     * @param columns        보일 칸의 순서
     * @param rateBarVisible 비율 칸의 왼쪽에 막대를 함께 그리는지 여부
     */
    record Layout(List<ReportColumn> columns, boolean rateBarVisible) {

        /**
         * 양식을 정할 수 없는 보고서(회계 연도 수입/지출 결산 보고서)가 쓰는 양식. 주간 보고서의 기본 양식과 같다.
         */
        static final Layout DEFAULT = new Layout(ReportTemplateTable.WEEKLY_INCOME_STATUS.getDefaultColumns(),
                ReportTemplateConstraints.DEFAULT_RATE_BAR_VISIBLE);
    }

    private static final String WIDEST_RATE = "999.9%";

    private BudgetStatusSection() {
        // 인스턴스화 방지
    }

    /**
     * 현재 위치에 수입 현황과 지출 현황을 나란히 그리고 그 아래로 이동한다. 현재 페이지에 다 들어가지 않으면 다음 페이지에 그린다.
     *
     * @param incomeLayout  수입 현황의 양식
     * @param expenseLayout 지출 현황의 양식
     * @param baseSize      표 글자의 기본 크기
     * @param titleSuffix   절 제목 뒤에 붙일 설명(집계 기준 등). 없으면 빈 문자열
     */
    static void render(ReportCanvas canvas, List<BudgetExecutionDto> incomeStatus,
            List<BudgetExecutionDto> expenseStatus, Layout incomeLayout, Layout expenseLayout, float baseSize,
            String titleSuffix) {
        double columnWidth = (canvas.getContentWidth() - COLUMN_GAP) / 2;
        double expenseLeft = canvas.getLeft() + columnWidth + COLUMN_GAP;

        ReportTable incomeTable = createTable(canvas.getStyle(), BudgetType.INCOME, incomeStatus, incomeLayout,
                baseSize, columnWidth);
        ReportTable expenseTable = createTable(canvas.getStyle(), BudgetType.EXPENSE, expenseStatus, expenseLayout,
                baseSize, columnWidth);

        double tableHeight = Math.max(incomeTable.measureHeight(columnWidth),
                expenseTable.measureHeight(columnWidth));
        if (!canvas.fits(canvas.sectionHeight(ReportFonts.SIZE_SECTION) + tableHeight)) {
            canvas.newPage();
        }

        double titleTop = canvas.getY();
        canvas.section("수입 현황" + titleSuffix, ReportFonts.SIZE_SECTION, canvas.getLeft());
        double tableTop = canvas.getY();
        canvas.setPosition(canvas.getPageIndex(), titleTop);
        canvas.section("지출 현황" + titleSuffix, ReportFonts.SIZE_SECTION, expenseLeft);

        incomeTable.renderAt(canvas, canvas.getLeft(), tableTop, columnWidth);
        expenseTable.renderAt(canvas, expenseLeft, tableTop, columnWidth);
        canvas.setPosition(canvas.getPageIndex(), tableTop + tableHeight);
    }

    /**
     * @param tableWidth 표의 폭. 칸마다 이 폭을 칸 수로 나눈 폭을 준다.
     */
    private static ReportTable createTable(ReportStyle style, BudgetType type, List<BudgetExecutionDto> rows,
            Layout layout, float baseSize, double tableWidth) {
        long totalBudgeted = 0;
        long totalActual = 0;
        for (BudgetExecutionDto row : rows) {
            totalBudgeted += row.getBudgetedAmount();
            totalActual += row.getActualAmount();
        }

        // 예산 항목은 나머지 폭을 차지하고 다른 칸에는 고른 폭(표의 폭 ÷ 칸 수)을 정해 둔다. 정해 둔 폭은 금액이나 비율이
        // 한 줄에 들어가지 않으면 넓어지므로(글자를 키운 경우) 금액과 비율은 줄을 바꾸지 않고, 그만큼 예산 항목이 좁아진다.
        double evenWidth = tableWidth / layout.columns()
                .size();
        List<Column> columns = new ArrayList<>();
        for (ReportColumn column : layout.columns()) {
            double width = column == ReportColumn.BUDGET_NAME ? 0 : evenWidth;
            columns.add(new Column(headerOf(column, type), width, alignOf(column)));
        }
        ReportTable table = new ReportTable(style, baseSize, columns.toArray(Column[]::new));

        // 막대의 길이가 행마다 같도록, 비율을 적을 자리는 가장 넓은 비율에 맞춘다.
        Font boldFont = style.bold(baseSize);
        double rateWidth = ReportText.width(WIDEST_RATE, boldFont);
        for (BudgetExecutionDto row : rows) {
            table.addRow(RowStyle.NORMAL, 0, cellsOf(layout, rateWidth, row.getName(), row.getActualAmount(),
                    row.getBudgetedAmount(), row.getExecutionRate()));
        }
        Double totalRate = totalBudgeted > 0 ? totalActual * 100.0 / totalBudgeted : null;
        table.addRow(RowStyle.TOTAL, 0, cellsOf(layout, rateWidth, "합계", totalActual, totalBudgeted, totalRate));
        return table;
    }

    /**
     * 칸의 머리글. 수입은 예산을 채운 정도(달성), 지출은 예산을 쓴 정도(집행)로 부른다.
     */
    private static String headerOf(ReportColumn column, BudgetType type) {
        ReportTemplateTable table = type == BudgetType.INCOME ? ReportTemplateTable.WEEKLY_INCOME_STATUS
                : ReportTemplateTable.WEEKLY_EXPENSE_STATUS;
        return table.labelOf(column);
    }

    private static ReportAlign alignOf(ReportColumn column) {
        return switch (column) {
        case BUDGET_NAME -> ReportAlign.LEFT;
        case RATE -> ReportAlign.CENTER;
        default -> ReportAlign.RIGHT;
        };
    }

    private static Cell[] cellsOf(Layout layout, double rateWidth, String name, long actual, long budgeted,
            Double rate) {
        return layout.columns()
                .stream()
                .map(column -> switch (column) {
                case BUDGET_NAME -> ReportTable.text(name);
                case ACTUAL_AMOUNT -> ReportTable.text(ReportFormats.amount(actual));
                case BUDGETED_AMOUNT -> ReportTable.text(ReportFormats.amount(budgeted));
                case RATE -> layout.rateBarVisible()
                        ? ReportTable.barText(toRatio(rate), ReportFormats.rate(rate), rateWidth)
                        : ReportTable.text(ReportFormats.rate(rate));
                default -> throw new IllegalArgumentException("현황 표에 없는 칸입니다: " + column);
                })
                .toArray(Cell[]::new);
    }

    /**
     * 백분율을 막대의 채움 비율(1이 가득)로 바꾼다.
     */
    private static Double toRatio(Double rate) {
        return rate == null ? null : rate / 100;
    }
}
