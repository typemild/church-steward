package org.church.steward.ui.view.report.composer;

import java.time.YearMonth;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 회계 연도 수입/지출 결산 보고서를 배치한다. 위에서부터 직전 회계 연도와 해당 회계 연도의 총괄, 월별 수입/지출(차트와 표), 수입
 * 현황과 지출 현황 순이다. 현황 표의 칸 순서와 막대는 보고서 양식({@link ReportTemplateDto})이 정한다.
 */
public final class AnnualSettlementReportComposer {

    private static final float BODY_SIZE = 9f;
    private static final float STATUS_SIZE = ReportFonts.SIZE_BODY;
    private static final float CHART_SIZE = ReportFonts.SIZE_SMALL;

    private static final double MONTH_WIDTH = ReportPaper.mm(13);
    /** 월별 표가 본문 폭에서 차지하는 비율. 나머지는 차트가 쓴다. */
    private static final double MONTHLY_TABLE_RATIO = 0.48;
    private static final double MONTHLY_GAP = ReportPaper.mm(4);

    private AnnualSettlementReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(AnnualSettlementReportDto report, ReportTemplateDto template,
            ReportStyle style) {
        String title = String.format("%s 회계 연도 수입/지출 결산 보고서", report.getFiscalYearName());

        ReportCanvas canvas = new ReportCanvas(title, style);
        canvas.title(title, ReportStyles.ANNUAL_TITLE_SIZE);
        canvas.moveDown(ReportStyles.TITLE_GAP);
        ReportStyles.drawUnit(canvas, ReportFonts.SIZE_SMALL);

        drawSummary(canvas, report);
        canvas.moveDown(ReportStyles.SECTION_SPACING);
        drawMonthly(canvas, report);
        canvas.moveDown(ReportStyles.SECTION_SPACING);
        BudgetStatusSection.render(canvas, report.getIncomeStatus(), report.getExpenseStatus(),
                BudgetStatusSection.Layout.of(template, ReportTemplateTable.ANNUAL_INCOME_STATUS),
                BudgetStatusSection.Layout.of(template, ReportTemplateTable.ANNUAL_EXPENSE_STATUS), STATUS_SIZE, "");

        return canvas.finish();
    }

    /**
     * 직전 회계 연도와 해당 회계 연도의 수입 총액, 지출 총액, 차액. 금액 칸은 세 칸 모두 가장 넓은 금액(머리글 포함)에 맞춘 같은
     * 폭이고, 회계 연도 칸이 남은 폭을 가져간다.
     */
    private static void drawSummary(ReportCanvas canvas, AnnualSettlementReportDto report) {
        canvas.section("수입/지출 총괄", ReportFonts.SIZE_SECTION, canvas.getLeft());

        ReportTable table = new ReportTable(canvas.getStyle(), BODY_SIZE,
                new Column("회계 연도", 0, ReportAlign.LEFT),
                new Column("수입 총액", 0, ReportAlign.RIGHT),
                new Column("지출 총액", 0, ReportAlign.RIGHT),
                new Column("차액", 0, ReportAlign.RIGHT));

        // 직전 회계 연도가 없으면 이름과 기간 대신 "없음"으로 적고 금액은 비워 둔다.
        String previous = report.getPreviousFiscalYearName() == null ? "없음"
                : String.format("%s, %s", report.getPreviousFiscalYearName(), ReportFormats.period(
                        report.getPreviousFiscalYearStartDate(), report.getPreviousFiscalYearEndDate()));
        addSummaryRow(table, RowStyle.NORMAL, String.format("직전 (%s)", previous), report.getPreviousYear());
        addSummaryRow(table, RowStyle.EMPHASIS, String.format("당해 (%s, %s)", report.getFiscalYearName(),
                ReportFormats.period(report.getFiscalYearStartDate(), report.getFiscalYearEndDate())),
                report.getCurrentYear());

        double amountWidth = 0;
        for (int i = 1; i <= 3; i++) {
            amountWidth = Math.max(amountWidth, table.measureNaturalWidth(i));
        }
        table.setWidths(0, amountWidth, amountWidth, amountWidth);
        table.render(canvas);
    }

    /**
     * @param summary 회계 연도의 총액. 직전 회계 연도가 없으면 null이며 금액을 비워 둔다.
     */
    private static void addSummaryRow(ReportTable table, RowStyle style, String name, ReportSummaryDto summary) {
        if (summary == null) {
            table.addRow(style, 0, name, ReportFormats.NONE, ReportFormats.NONE, ReportFormats.NONE);
            return;
        }
        table.addRow(style, 0, name,
                ReportFormats.amount(summary.getIncome()),
                ReportFormats.amount(summary.getExpense()),
                ReportFormats.amount(summary.getDifference()));
    }

    /**
     * 월별 수입/지출. 왼쪽에 흐름을 보여주는 차트를, 오른쪽에 정확한 금액을 적은 표를 같은 높이로 놓는다.
     */
    private static void drawMonthly(ReportCanvas canvas, AnnualSettlementReportDto report) {
        ReportTable table = new ReportTable(canvas.getStyle(), BODY_SIZE,
                new Column("월", MONTH_WIDTH, ReportAlign.CENTER),
                new Column("수입", 0, ReportAlign.RIGHT),
                new Column("지출", 0, ReportAlign.RIGHT),
                new Column("차액", 0, ReportAlign.RIGHT));

        boolean withYear = MonthlyTrendChart.needsYear(report.getMonthlyTrend());
        long totalIncome = 0;
        long totalExpense = 0;
        for (MonthlyTrendDto month : report.getMonthlyTrend()) {
            totalIncome += month.getIncome();
            totalExpense += month.getExpense();
            table.addRow(RowStyle.NORMAL, 0,
                    MonthlyTrendChart.fullLabelOf(YearMonth.parse(month.getYearMonth()), withYear),
                    ReportFormats.amount(month.getIncome()),
                    ReportFormats.amount(month.getExpense()),
                    ReportFormats.amount(month.getIncome() - month.getExpense()));
        }
        table.addRow(RowStyle.TOTAL, 0,
                "합계",
                ReportFormats.amount(totalIncome),
                ReportFormats.amount(totalExpense),
                ReportFormats.amount(totalIncome - totalExpense));

        double tableWidth = canvas.getContentWidth() * MONTHLY_TABLE_RATIO;
        double chartWidth = canvas.getContentWidth() - tableWidth - MONTHLY_GAP;
        double height = table.measureHeight(tableWidth);
        if (!canvas.fits(canvas.sectionHeight(ReportFonts.SIZE_SECTION) + height)) {
            canvas.newPage();
        }

        canvas.section("월별 수입/지출", ReportFonts.SIZE_SECTION, canvas.getLeft());
        double top = canvas.getY();
        MonthlyTrendChart.draw(canvas, canvas.getLeft(), top, chartWidth, height, report.getMonthlyTrend(),
                CHART_SIZE);
        table.renderAt(canvas, canvas.getLeft() + chartWidth + MONTHLY_GAP, top, tableWidth);
        canvas.setPosition(canvas.getPageIndex(), top + height);
    }
}
