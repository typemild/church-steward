package org.church.steward.ui.view.report.composer;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 주간 회계 결산 보고서를 배치한다. 위에서부터 제목·회계 연도·결산 기간과 결재란, 개요(전주 잔액, 주간 수입/지출/차액, 현재 잔액),
 * 수입 현황과 지출 현황, 주간 수입 내역과 주간 지출 내역 순이다. 가능한 한 A4 한 장에 담기도록 기본 글자는 12pt 이하로, 간격은
 * 좁게 쓴다.
 * <p>
 * 결재란, 현황 표와 내역의 칸 순서와 숨김은 보고서 양식({@link ReportTemplateDto})이 정한다.
 */
public final class WeeklySettlementReportComposer {

    private static final String TITLE = "주간 회계 결산 보고서";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd(E)", Locale.KOREAN);
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private static final float OVERVIEW_SIZE = 9f;
    private static final float STATUS_SIZE = ReportFonts.SIZE_DETAIL;

    private static final double HEADER_GAP = 4;
    private static final double SECTION_SPACING = 7;

    private WeeklySettlementReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(WeeklySettlementReportDto report, ReportTemplateDto template,
            ReportStyle style) {
        // 파일 이름으로도 쓰이므로 어느 주의 보고서인지 알 수 있게 회계 연도와 기간을 넣는다.
        String documentTitle = String.format("%s %s (%s ~ %s)", report.getFiscalYearName(), TITLE,
                report.getStartDate(), report.getEndDate());

        ReportCanvas canvas = new ReportCanvas(documentTitle, style);
        drawHeader(canvas, report, template.getApproval());
        canvas.moveDown(HEADER_GAP);
        ReportStyles.drawUnit(canvas, ReportFonts.SIZE_SMALL);
        drawOverview(canvas, report);
        canvas.moveDown(SECTION_SPACING);

        String statusSuffix = String.format(" (%s까지 누계)", report.getEndDate()
                .format(SHORT_DATE_FORMAT));
        BudgetStatusSection.render(canvas, report.getIncomeStatus(), report.getExpenseStatus(),
                BudgetStatusSection.Layout.of(template, ReportTemplateTable.WEEKLY_INCOME_STATUS),
                BudgetStatusSection.Layout.of(template, ReportTemplateTable.WEEKLY_EXPENSE_STATUS), STATUS_SIZE,
                statusSuffix);
        canvas.moveDown(SECTION_SPACING);

        new WeeklyRecordSection(style).render(canvas, report.getIncomeRecords(), report.getExpenseRecords(), template);
        return canvas.finish();
    }

    /**
     * 왼쪽에 제목과 회계 연도·결산 기간을, 오른쪽에 결재란을 놓는다({@link ReportHeader}).
     */
    private static void drawHeader(ReportCanvas canvas, WeeklySettlementReportDto report, ReportApprovalDto approval) {
        String fiscalYear = String.format("%s (%s ~ %s)", report.getFiscalYearName(),
                report.getFiscalYearStartDate(), report.getFiscalYearEndDate());
        String period = String.format("%s ~ %s", report.getStartDate()
                .format(DATE_FORMAT),
                report.getEndDate()
                        .format(DATE_FORMAT));
        ReportHeader.draw(canvas, TITLE, ReportFonts.SIZE_TITLE, List.of(new ReportHeader.Info("회계 연도 : ",
                fiscalYear), new ReportHeader.Info("결산 기간 : ", period)), approval);
    }

    /**
     * 개요. 전주 잔액에서 주간 수입과 지출을 거쳐 현재 잔액에 이르는 흐름을 한 줄로 보여준다.
     */
    private static void drawOverview(ReportCanvas canvas, WeeklySettlementReportDto report) {
        ReportTable table = new ReportTable(canvas.getStyle(), OVERVIEW_SIZE,
                new Column("전주 잔액", 0, ReportAlign.CENTER),
                new Column("주간 수입", 0, ReportAlign.CENTER),
                new Column("주간 지출", 0, ReportAlign.CENTER),
                new Column("주간 차액", 0, ReportAlign.CENTER),
                new Column("현재 잔액", 0, ReportAlign.CENTER));

        table.addRow(RowStyle.EMPHASIS, 0,
                ReportFormats.amount(report.getPreviousBalance()),
                ReportFormats.amount(report.getWeekly()
                        .getIncome()),
                ReportFormats.amount(report.getWeekly()
                        .getExpense()),
                ReportFormats.amount(report.getWeekly()
                        .getDifference()),
                ReportFormats.amount(report.getCurrentBalance()));
        table.render(canvas);
    }
}
