package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.ui.util.UIColors;
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
 * 주간 회계 결산 보고서를 배치한다.
 * 위에서부터 제목·기간과 결재란, 개요(전주 잔액, 주간 수입/지출/차액, 현재 잔액), 수입 현황과 지출 현황, 수입 내역과 지출 내역
 * 순이다.
 * 가능한 한 A4 한 장에 담기도록 기본 글자는 12pt 이하로, 간격은 좁게 쓴다.
 */
public final class WeeklySettlementReportComposer {

    private static final String TITLE = "주간 회계 결산 보고서";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd(E)", Locale.KOREAN);
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private static final float INFO_SIZE = 9f;
    private static final float OVERVIEW_SIZE = 9f;
    private static final float STATUS_SIZE = ReportFonts.SIZE_DETAIL;

    /** 결재란. 직함을 손으로 쓰는 위 칸과 서명하는 아래 칸이 세 벌 있다. */
    private static final int APPROVAL_COUNT = 3;
    private static final double APPROVAL_LABEL_WIDTH = ReportPaper.mm(6);
    private static final double APPROVAL_CELL_WIDTH = ReportPaper.mm(20);
    private static final double APPROVAL_TITLE_HEIGHT = ReportPaper.mm(5);
    private static final double APPROVAL_SIGN_HEIGHT = ReportPaper.mm(13);
    private static final double APPROVAL_LINE = 0.5;

    private static final double HEADER_GAP = 4;
    private static final double SECTION_SPACING = 7;

    private WeeklySettlementReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(WeeklySettlementReportDto report, ReportStyle style) {
        // 파일 이름으로도 쓰이므로 어느 주의 보고서인지 알 수 있게 회계 연도와 기간을 넣는다.
        String documentTitle = String.format("%s %s (%s ~ %s)", report.getFiscalYearName(), TITLE,
                report.getStartDate(), report.getEndDate());

        ReportCanvas canvas = new ReportCanvas(documentTitle, style);
        drawHeader(canvas, report);
        canvas.moveDown(HEADER_GAP);
        ReportStyles.drawUnit(canvas, ReportFonts.SIZE_SMALL);
        drawOverview(canvas, report);
        canvas.moveDown(SECTION_SPACING);

        String statusSuffix = String.format(" (%s까지 누계)", report.getEndDate()
                .format(SHORT_DATE_FORMAT));
        BudgetStatusSection.render(canvas, report.getIncomeStatus(), report.getExpenseStatus(), STATUS_SIZE,
                statusSuffix);
        canvas.moveDown(SECTION_SPACING);

        new WeeklyRecordSection(style).render(canvas, report.getIncomeRecords(), report.getExpenseRecords());
        return canvas.finish();
    }

    /**
     * 왼쪽에 제목과 회계 연도·기간을, 오른쪽에 결재란을 놓는다.
     * 높이는 결재란에 맞추되, 글자를 키워 제목과 두 줄의 회계 연도·기간이 들어가지 않으면 그만큼 늘린다.
     */
    private static void drawHeader(ReportCanvas canvas, WeeklySettlementReportDto report) {
        double top = canvas.getY();

        ReportStyle style = canvas.getStyle();
        Font titleFont = style.bold(ReportFonts.SIZE_TITLE);
        Font infoFont = style.regular(INFO_SIZE);
        double infoLineHeight = ReportText.lineHeight(infoFont);
        double height = Math.max(APPROVAL_TITLE_HEIGHT + APPROVAL_SIGN_HEIGHT,
                ReportText.lineHeight(titleFont) + HEADER_GAP + infoLineHeight * 2);

        canvas.text(TITLE, titleFont, ReportAlign.LEFT, canvas.getLeft(), top + ReportText.ascent(titleFont));

        // 회계 연도와 기간은 결재란의 아래쪽에 맞춰 두 줄로 적는다.
        double periodBaseline = top + height - infoLineHeight + ReportText.ascent(infoFont);
        canvas.text(String.format("회계 연도 : %s", report.getFiscalYearName()), infoFont, ReportAlign.LEFT,
                canvas.getLeft(), periodBaseline - infoLineHeight);
        canvas.text(String.format("기      간 : %s ~ %s", report.getStartDate()
                .format(DATE_FORMAT),
                report.getEndDate()
                        .format(DATE_FORMAT)),
                infoFont, ReportAlign.LEFT, canvas.getLeft(), periodBaseline);

        drawApprovalBox(canvas, top);
        canvas.moveDown(height);
    }

    /**
     * 페이지의 오른쪽 위에 결재란을 그린다. 직함 칸은 손으로 쓸 수 있게 비워 둔다.
     */
    private static void drawApprovalBox(ReportCanvas canvas, double top) {
        double width = APPROVAL_LABEL_WIDTH + APPROVAL_CELL_WIDTH * APPROVAL_COUNT;
        double height = APPROVAL_TITLE_HEIGHT + APPROVAL_SIGN_HEIGHT;
        double left = canvas.getRight() - width;
        double cellsLeft = left + APPROVAL_LABEL_WIDTH;

        canvas.strokeRect(left, top, width, height, APPROVAL_LINE, UIColors.REPORT_LINE);
        for (int i = 0; i < APPROVAL_COUNT; i++) {
            double x = cellsLeft + APPROVAL_CELL_WIDTH * i;
            canvas.line(x, top, x, top + height, APPROVAL_LINE, UIColors.REPORT_LINE);
        }
        canvas.line(cellsLeft, top + APPROVAL_TITLE_HEIGHT, left + width, top + APPROVAL_TITLE_HEIGHT, APPROVAL_LINE,
                UIColors.REPORT_LINE);

        // "결재"를 세로로 한 글자씩 적는다.
        Font labelFont = canvas.getStyle()
                .regular(ReportFonts.SIZE_BODY);
        double labelCenter = left + APPROVAL_LABEL_WIDTH / 2;
        double middle = top + height / 2;
        canvas.text("결", labelFont, ReportAlign.CENTER, labelCenter, middle - 3);
        canvas.text("재", labelFont, ReportAlign.CENTER, labelCenter, middle + 3 + ReportText.ascent(labelFont));
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
