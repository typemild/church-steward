package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.util.List;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 결재란. 왼쪽에 세로로 적은 "결재"가 있고, 직함을 적는 위 칸과 서명하는 아래 칸이 칸 수만큼 있다. 주간 회계 결산 보고서와 회계
 * 연도 결산 보고서가 함께 쓰며, 칸 수와 직함은 보고서마다 양식에서 따로 정한다.
 */
final class ApprovalBox {

    private static final double LABEL_WIDTH = ReportPaper.mm(6);
    private static final double CELL_WIDTH = ReportPaper.mm(20);
    private static final double TITLE_HEIGHT = ReportPaper.mm(5);
    private static final double SIGN_HEIGHT = ReportPaper.mm(13);
    /** 직함 칸의 글자 둘레 여백 */
    private static final double TITLE_PADDING = 2;
    private static final double LINE = 0.5;

    private ApprovalBox() {
        // 인스턴스화 방지
    }

    /**
     * 결재란의 폭
     */
    static double width(int cellCount) {
        return LABEL_WIDTH + CELL_WIDTH * cellCount;
    }

    /**
     * 결재란의 높이. 직함 칸은 글자를 키우면 글자가 들어가도록 높아진다.
     */
    static double height(ReportStyle style) {
        return titleHeight(style) + SIGN_HEIGHT;
    }

    private static double titleHeight(ReportStyle style) {
        return Math.max(TITLE_HEIGHT, ReportText.lineHeight(titleFont(style)) + TITLE_PADDING * 2);
    }

    private static Font titleFont(ReportStyle style) {
        return style.regular(ReportFonts.SIZE_BODY);
    }

    /**
     * 오른쪽 끝을 맞춰 결재란을 그린다. 작업대의 위치는 바꾸지 않는다. 직함이 없는 칸은 손으로 쓸 수 있게 비워 두고, 칸에 들어가지 않는
     * 직함은 말줄임표 없이 글자를 작게 줄여 모두 적는다.
     *
     * @param right  결재란의 오른쪽 끝
     * @param titles 칸마다의 직함. 개수가 칸 수다.
     */
    static void draw(ReportCanvas canvas, double right, double top, List<String> titles) {
        ReportStyle style = canvas.getStyle();
        Font titleFont = titleFont(style);
        double titleHeight = titleHeight(style);
        int cellCount = titles.size();
        double width = width(cellCount);
        double height = titleHeight + SIGN_HEIGHT;
        double left = right - width;
        double cellsLeft = left + LABEL_WIDTH;

        canvas.strokeRect(left, top, width, height, LINE, UIColors.REPORT_LINE);
        for (int i = 0; i < cellCount; i++) {
            double x = cellsLeft + CELL_WIDTH * i;
            canvas.line(x, top, x, top + height, LINE, UIColors.REPORT_LINE);
        }
        canvas.line(cellsLeft, top + titleHeight, left + width, top + titleHeight, LINE, UIColors.REPORT_LINE);

        double titleBaseline = top + (titleHeight - ReportText.lineHeight(titleFont)) / 2 + ReportText.ascent(
                titleFont);
        for (int i = 0; i < cellCount; i++) {
            canvas.shrinkingText(titles.get(i), titleFont, ReportAlign.CENTER,
                    cellsLeft + CELL_WIDTH * i + CELL_WIDTH / 2, titleBaseline, CELL_WIDTH - TITLE_PADDING * 2);
        }

        // "결재"를 세로로 한 글자씩 적는다.
        Font labelFont = style.regular(ReportFonts.SIZE_BODY);
        double labelCenter = left + LABEL_WIDTH / 2;
        double middle = top + height / 2;
        canvas.text("결", labelFont, ReportAlign.CENTER, labelCenter, middle - 3);
        canvas.text("재", labelFont, ReportAlign.CENTER, labelCenter, middle + 3 + ReportText.ascent(labelFont));
    }
}
