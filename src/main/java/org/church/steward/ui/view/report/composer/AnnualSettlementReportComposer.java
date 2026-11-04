package org.church.steward.ui.view.report.composer;

import java.time.YearMonth;
import java.util.List;

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
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 회계 연도 수입/지출 결산 보고서를 배치한다. 위에서부터 머리 부분(왼쪽에 제목과 회계 연도·회계 기간, 오른쪽에 결재란. 주간 회계 결산
 * 보고서와 같은 배치), 직전 회계 연도와 해당 회계 연도의 총괄, 월별 수입/지출(차트와 표), 수입 현황과 지출 현황 순이다. 결재란과
 * 현황 표의 칸 순서·막대는 보고서 양식({@link ReportTemplateDto})이 정하며, 결재란은 주간 회계 결산 보고서와 따로
 * 정한다.
 */
public final class AnnualSettlementReportComposer {

    private static final String TITLE = "회계 연도 수입/지출 결산 보고서";

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
        // 파일 이름으로도 쓰이므로 문서 제목에는 어느 회계 연도의 보고서인지 넣는다. 지면의 제목에는 넣지 않고 아래 줄에 적는다.
        String documentTitle = String.format("%s %s", report.getFiscalYearName(), TITLE);

        ReportCanvas canvas = new ReportCanvas(documentTitle, style);
        ReportHeader.draw(canvas, TITLE, ReportStyles.ANNUAL_TITLE_SIZE, List.of(
                new ReportHeader.Info("회계 연도 : ", report.getFiscalYearName()),
                new ReportHeader.Info("회계 기간 : ", ReportFormats.period(report.getFiscalYearStartDate(),
                        report.getFiscalYearEndDate()))),
                template.getApproval());
        canvas.moveDown(ReportStyles.SECTION_SPACING);

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
     * 직전 회계 연도와 해당 회계 연도의 수입 총액, 지출 총액, 차액. 절 제목과 같은 줄의 오른쪽 끝에 금액의 단위를 적는다.
     * <p>
     * 칸은 내용에 맞춘 폭(금액 칸은 세 칸 모두 가장 넓은 금액이나 머리글에 맞춘 같은 폭)에서 시작해 남는 폭을 네 칸이 똑같이 나눠
     * 갖는다. 금액은 오른쪽 정렬한 덩어리로 칸 가운데에 놓아 칸이 넓어져도 머리글과 멀어지지 않게 한다.
     */
    private static void drawSummary(ReportCanvas canvas, AnnualSettlementReportDto report) {
        double sectionBaseline = canvas.getY() + ReportText.ascent(canvas.getStyle()
                .bold(ReportFonts.SIZE_SECTION));
        canvas.section("수입/지출 총괄", ReportFonts.SIZE_SECTION, canvas.getLeft());
        ReportStyles.drawUnitAt(canvas, ReportFonts.SIZE_SMALL, sectionBaseline);

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
            table.setBlockCentered(i);
        }
        double nameWidth = table.measureNaturalWidth(0);
        double extra = (canvas.getContentWidth() - nameWidth - amountWidth * 3) / 4;
        if (extra >= 0) {
            table.setWidths(nameWidth + extra, amountWidth + extra, amountWidth + extra, amountWidth + extra);
        } else {
            // 회계 연도 칸이 한 줄에 들어가지 않을 만큼 좁으면(글자를 아주 키운 경우) 남는 폭에서 줄을 바꾼다.
            table.setWidths(0, amountWidth, amountWidth, amountWidth);
        }
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
