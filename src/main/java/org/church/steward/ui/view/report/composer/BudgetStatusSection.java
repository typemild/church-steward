package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 수입 현황과 지출 현황. 최상위 예산별로 예산액 대비 실제 금액을 막대와 숫자로 함께 보여준다.
 * 대시보드의 "예산 집행 현황"과 같은 내용이지만, 종이에서는 툴팁을 볼 수 없으므로 금액과 비율을 모두 적는다.
 * 두 현황은 좌우로 나란히 놓는다.
 */
final class BudgetStatusSection {

    /** 수입 현황과 지출 현황 사이의 간격 */
    static final double COLUMN_GAP = ReportPaper.mm(4);

    private static final double BAR_WIDTH = ReportPaper.mm(13);
    /** 칸의 좌우 여백을 합친 폭 */
    private static final double CELL_PADDING = 7;
    private static final String WIDEST_RATE = "999.9%";

    private BudgetStatusSection() {
        // 인스턴스화 방지
    }

    /**
     * 현재 위치에 수입 현황과 지출 현황을 나란히 그리고 그 아래로 이동한다. 현재 페이지에 다 들어가지 않으면 다음 페이지에 그린다.
     *
     * @param baseSize    표 글자의 기본 크기
     * @param titleSuffix 절 제목 뒤에 붙일 설명(집계 기준 등). 없으면 빈 문자열
     */
    static void render(ReportCanvas canvas, List<BudgetExecutionDto> incomeStatus,
            List<BudgetExecutionDto> expenseStatus, float baseSize, String titleSuffix) {
        double columnWidth = (canvas.getContentWidth() - COLUMN_GAP) / 2;
        double expenseLeft = canvas.getLeft() + columnWidth + COLUMN_GAP;

        ReportTable incomeTable = createTable(canvas.getStyle(), BudgetType.INCOME, incomeStatus, baseSize);
        ReportTable expenseTable = createTable(canvas.getStyle(), BudgetType.EXPENSE, expenseStatus, baseSize);

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

    private static ReportTable createTable(ReportStyle style, BudgetType type, List<BudgetExecutionDto> rows,
            float baseSize) {
        boolean income = type == BudgetType.INCOME;

        long totalBudgeted = 0;
        long totalActual = 0;
        for (BudgetExecutionDto row : rows) {
            totalBudgeted += row.getBudgetedAmount();
            totalActual += row.getActualAmount();
        }

        // 금액 열은 가장 큰 금액(합계)이 들어갈 만큼만 잡아 예산명에 폭을 최대한 준다.
        Font boldFont = style.bold(baseSize);
        double amountWidth = CELL_PADDING + Math.max(
                ReportText.width(ReportFormats.amount(Math.max(totalBudgeted, totalActual)), boldFont),
                ReportText.width("예산액", boldFont));
        double rateWidth = CELL_PADDING + ReportText.width(WIDEST_RATE, boldFont);

        // 수입은 예산을 채운 정도(달성), 지출은 예산을 쓴 정도(집행)로 부른다.
        ReportTable table = new ReportTable(style, baseSize,
                new Column("예산 항목", 0, ReportAlign.LEFT),
                new Column("", BAR_WIDTH, ReportAlign.LEFT),
                new Column(income ? "수입액" : "집행액", amountWidth, ReportAlign.RIGHT),
                new Column("예산액", amountWidth, ReportAlign.RIGHT),
                new Column(income ? "달성률" : "집행률", rateWidth, ReportAlign.RIGHT));

        for (BudgetExecutionDto row : rows) {
            table.addRow(RowStyle.NORMAL, 0,
                    ReportTable.text(row.getName()),
                    ReportTable.bar(toRatio(row.getExecutionRate())),
                    ReportTable.text(ReportFormats.amount(row.getActualAmount())),
                    ReportTable.text(ReportFormats.amount(row.getBudgetedAmount())),
                    ReportTable.text(ReportFormats.rate(row.getExecutionRate())));
        }

        Double totalRate = totalBudgeted > 0 ? totalActual * 100.0 / totalBudgeted : null;
        table.addRow(RowStyle.TOTAL, 0,
                ReportTable.text("합계"),
                ReportTable.bar(toRatio(totalRate)),
                ReportTable.text(ReportFormats.amount(totalActual)),
                ReportTable.text(ReportFormats.amount(totalBudgeted)),
                ReportTable.text(ReportFormats.rate(totalRate)));
        return table;
    }

    /**
     * 백분율을 막대의 채움 비율(1이 가득)로 바꾼다.
     */
    private static Double toRatio(Double rate) {
        return rate == null ? null : rate / 100;
    }
}
