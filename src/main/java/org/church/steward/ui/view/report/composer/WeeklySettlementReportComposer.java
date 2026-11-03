package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
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

    private static final float INFO_SIZE = 9f;
    /** 머리 부분의 정보(회계 연도, 결산 기간) 줄 높이의 글자 높이에 대한 배수. 서식의 행 간격과 무관하다. */
    private static final double INFO_LINE_SPACING = 1.5;
    private static final float OVERVIEW_SIZE = 9f;
    private static final float STATUS_SIZE = ReportFonts.SIZE_DETAIL;

    /** 결재란. 직함을 적는 위 칸과 서명하는 아래 칸이 칸 수만큼 있다. */
    private static final double APPROVAL_LABEL_WIDTH = ReportPaper.mm(6);
    private static final double APPROVAL_CELL_WIDTH = ReportPaper.mm(20);
    private static final double APPROVAL_TITLE_HEIGHT = ReportPaper.mm(5);
    private static final double APPROVAL_SIGN_HEIGHT = ReportPaper.mm(13);
    /** 직함 칸의 글자 둘레 여백 */
    private static final double APPROVAL_TITLE_PADDING = 2;
    private static final double APPROVAL_LINE = 0.5;
    /** 머리 부분의 글자와 결재란 사이의 최소 간격 */
    private static final double APPROVAL_SPACING = ReportPaper.mm(3);

    private static final double HEADER_GAP = 4;
    private static final double SECTION_SPACING = 7;

    /**
     * 머리 부분의 정보 한 줄. 값이 넘치면 이름 뒤에 맞춰 줄을 바꾼다.
     */
    private record InfoRow(String prefix, List<String> lines) {
    }

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
                statusLayout(template, ReportTemplateTable.WEEKLY_INCOME_STATUS),
                statusLayout(template, ReportTemplateTable.WEEKLY_EXPENSE_STATUS), STATUS_SIZE, statusSuffix);
        canvas.moveDown(SECTION_SPACING);

        new WeeklyRecordSection(style).render(canvas, report.getIncomeRecords(), report.getExpenseRecords(),
                template.getTables()
                        .get(ReportTemplateTable.WEEKLY_INCOME_RECORDS),
                template.getTables()
                        .get(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS));
        return canvas.finish();
    }

    private static BudgetStatusSection.Layout statusLayout(ReportTemplateDto template, ReportTemplateTable table) {
        List<ReportColumn> columns = template.getTables()
                .get(table)
                .stream()
                .filter(column -> !column.isHidden())
                .map(ReportColumnDto::getColumn)
                .toList();
        return new BudgetStatusSection.Layout(columns, template.getRateBarVisible()
                .get(table));
    }

    /**
     * 왼쪽에 제목과 회계 연도·결산 기간을, 오른쪽에 결재란을 놓는다. 높이는 결재란을 숨겨도 결재란에 맞추되, 제목과 두 줄의 정보가 들어가지
     * 않으면(글자를 키운 경우) 그만큼으로 한다. 글자는 결재란과 겹치지 않도록 결재란 왼쪽의 폭에서 줄을 바꾼다.
     */
    private static void drawHeader(ReportCanvas canvas, WeeklySettlementReportDto report, ReportApprovalDto approval) {
        double top = canvas.getY();
        boolean approvalVisible = approval != null && approval.isVisible();

        ReportStyle style = canvas.getStyle();
        Font titleFont = style.bold(ReportFonts.SIZE_TITLE);
        Font infoFont = style.regular(INFO_SIZE);
        Font approvalFont = style.regular(ReportFonts.SIZE_BODY);
        double titleLineHeight = ReportText.lineHeight(titleFont);
        // 정보 줄 사이는 글자가 맞닿지 않게 띄운다. 맨 아랫줄 아래에는 띄우지 않아 아래쪽 끝이 결재란과 맞는다.
        double infoTextHeight = ReportText.lineHeight(infoFont);
        double infoLineHeight = infoTextHeight * INFO_LINE_SPACING;
        double approvalTitleHeight = Math.max(APPROVAL_TITLE_HEIGHT,
                ReportText.lineHeight(approvalFont) + APPROVAL_TITLE_PADDING * 2);

        double approvalWidth = approvalVisible ? APPROVAL_LABEL_WIDTH + APPROVAL_CELL_WIDTH * approval.getTitles()
                .size() : 0;
        double textWidth = approvalVisible ? canvas.getContentWidth() - approvalWidth - APPROVAL_SPACING
                : canvas.getContentWidth();

        List<String> titleLines = ReportText.wrap(TITLE, titleFont, textWidth);
        List<InfoRow> infoRows = toInfoRows(report, infoFont, textWidth);
        int infoLineCount = 0;
        for (InfoRow row : infoRows) {
            infoLineCount += row.lines()
                    .size();
        }

        double infoHeight = infoLineHeight * (infoLineCount - 1) + infoTextHeight;
        double textHeight = titleLineHeight * titleLines.size() + HEADER_GAP + infoHeight;
        // 결재란을 숨겨도 높이는 결재란이 있을 때와 같게 두어, 제목과 정보 사이의 간격이 달라지지 않게 한다.
        double height = Math.max(approvalTitleHeight + APPROVAL_SIGN_HEIGHT, textHeight);

        double titleBaseline = top + ReportText.ascent(titleFont);
        for (String line : titleLines) {
            canvas.text(line, titleFont, ReportAlign.LEFT, canvas.getLeft(), titleBaseline);
            titleBaseline += titleLineHeight;
        }

        // 회계 연도와 결산 기간은 머리 부분의 아래쪽(결재란의 아래쪽)에 맞춰 적는다.
        double prefixWidth = prefixWidth(infoRows, infoFont);
        double infoBaseline = top + height - infoHeight + ReportText.ascent(infoFont);
        for (InfoRow row : infoRows) {
            canvas.text(row.prefix(), infoFont, ReportAlign.LEFT, canvas.getLeft(), infoBaseline);
            for (String line : row.lines()) {
                canvas.text(line, infoFont, ReportAlign.LEFT, canvas.getLeft() + prefixWidth, infoBaseline);
                infoBaseline += infoLineHeight;
            }
        }

        if (approvalVisible) {
            drawApprovalBox(canvas, top, approval.getTitles(), approvalFont, approvalTitleHeight);
        }
        canvas.moveDown(height);
    }

    /**
     * 머리 부분의 정보. 값은 이름("회계 연도 : ") 뒤의 폭에서 줄을 바꾼다.
     */
    private static List<InfoRow> toInfoRows(WeeklySettlementReportDto report, Font font, double textWidth) {
        String fiscalYear = String.format("%s (%s ~ %s)", report.getFiscalYearName(),
                report.getFiscalYearStartDate(), report.getFiscalYearEndDate());
        String period = String.format("%s ~ %s", report.getStartDate()
                .format(DATE_FORMAT),
                report.getEndDate()
                        .format(DATE_FORMAT));

        List<String> prefixes = List.of("회계 연도 : ", "결산 기간 : ");
        List<String> values = List.of(fiscalYear, period);
        double valueWidth = textWidth - Math.max(ReportText.width(prefixes.get(0), font),
                ReportText.width(prefixes.get(1), font));

        List<InfoRow> rows = new ArrayList<>();
        for (int i = 0; i < prefixes.size(); i++) {
            rows.add(new InfoRow(prefixes.get(i), ReportText.wrap(values.get(i), font, valueWidth)));
        }
        return rows;
    }

    private static double prefixWidth(List<InfoRow> rows, Font font) {
        double width = 0;
        for (InfoRow row : rows) {
            width = Math.max(width, ReportText.width(row.prefix(), font));
        }
        return width;
    }

    /**
     * 페이지의 오른쪽 위에 결재란을 그린다. 직함이 없는 칸은 손으로 쓸 수 있게 비워 두고, 칸에 들어가지 않는 직함은 말줄임표 없이 글자를
     * 작게 줄여 모두 적는다.
     *
     * @param titles 칸마다의 직함. 개수가 칸 수다.
     */
    private static void drawApprovalBox(ReportCanvas canvas, double top, List<String> titles, Font titleFont,
            double titleHeight) {
        int cellCount = titles.size();
        double width = APPROVAL_LABEL_WIDTH + APPROVAL_CELL_WIDTH * cellCount;
        double height = titleHeight + APPROVAL_SIGN_HEIGHT;
        double left = canvas.getRight() - width;
        double cellsLeft = left + APPROVAL_LABEL_WIDTH;

        canvas.strokeRect(left, top, width, height, APPROVAL_LINE, UIColors.REPORT_LINE);
        for (int i = 0; i < cellCount; i++) {
            double x = cellsLeft + APPROVAL_CELL_WIDTH * i;
            canvas.line(x, top, x, top + height, APPROVAL_LINE, UIColors.REPORT_LINE);
        }
        canvas.line(cellsLeft, top + titleHeight, left + width, top + titleHeight, APPROVAL_LINE,
                UIColors.REPORT_LINE);

        double titleBaseline = top + (titleHeight - ReportText.lineHeight(titleFont)) / 2 + ReportText.ascent(
                titleFont);
        for (int i = 0; i < cellCount; i++) {
            canvas.shrinkingText(titles.get(i), titleFont, ReportAlign.CENTER,
                    cellsLeft + APPROVAL_CELL_WIDTH * i + APPROVAL_CELL_WIDTH / 2, titleBaseline,
                    APPROVAL_CELL_WIDTH - APPROVAL_TITLE_PADDING * 2);
        }

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
