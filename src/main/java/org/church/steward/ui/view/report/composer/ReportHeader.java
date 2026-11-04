package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 결재란이 있는 보고서의 머리 부분. 왼쪽에 제목과 그 아래 정보 줄(회계 연도 등)을, 오른쪽에 결재란을 놓는다. 주간 회계 결산 보고서와
 * 회계 연도 결산 보고서가 함께 쓴다.
 * <p>
 * 높이는 결재란을 숨겨도 결재란에 맞추되, 제목과 정보가 들어가지 않으면(글자를 키운 경우) 그만큼으로 한다. 결재란을 숨겨도 높이를 같게
 * 두어 제목과 정보 사이의 간격이 달라지지 않게 한다. 정보 줄은 머리 부분의 아래쪽(결재란의 아래쪽)에 맞춰 적는다. 글자는 결재란과
 * 겹치지 않도록 결재란 왼쪽의 폭에서 줄을 바꾸며, 정보의 값은 이름("회계 연도 : ") 뒤에 맞춰 줄을 바꾼다.
 */
final class ReportHeader {

    /**
     * 정보 한 줄
     *
     * @param label 이름. "회계 연도 : "처럼 뒤의 구분까지 적는다.
     */
    record Info(String label, String value) {
    }

    /**
     * 줄바꿈까지 마친 정보 한 줄
     */
    private record InfoRow(String label, List<String> lines) {
    }

    private static final float INFO_SIZE = 9f;
    /** 정보 줄 높이의 글자 높이에 대한 배수. 서식의 행 간격과 무관하다. */
    private static final double INFO_LINE_SPACING = 1.5;
    /** 제목과 정보 사이의 간격 */
    private static final double TITLE_GAP = 4;
    /** 머리 부분의 글자와 결재란 사이의 최소 간격 */
    private static final double APPROVAL_SPACING = ReportPaper.mm(3);

    private ReportHeader() {
        // 인스턴스화 방지
    }

    /**
     * 현재 위치에 머리 부분을 그리고 그 아래로 이동한다.
     *
     * @param titleSize 제목 글자의 기본 크기
     * @param approval  결재란 양식. 결재란이 없으면 null
     */
    static void draw(ReportCanvas canvas, String title, float titleSize, List<Info> infos,
            ReportApprovalDto approval) {
        double top = canvas.getY();
        boolean approvalVisible = approval != null && approval.isVisible();

        ReportStyle style = canvas.getStyle();
        Font titleFont = style.bold(titleSize);
        Font infoFont = style.regular(INFO_SIZE);
        double titleLineHeight = ReportText.lineHeight(titleFont);
        // 정보 줄 사이는 글자가 맞닿지 않게 띄운다. 맨 아랫줄 아래에는 띄우지 않아 아래쪽 끝이 결재란과 맞는다.
        double infoTextHeight = ReportText.lineHeight(infoFont);
        double infoLineHeight = infoTextHeight * INFO_LINE_SPACING;

        double textWidth = approvalVisible ? canvas.getContentWidth() - ApprovalBox.width(approval.getTitles()
                .size()) - APPROVAL_SPACING : canvas.getContentWidth();

        List<String> titleLines = ReportText.wrap(title, titleFont, textWidth);
        List<InfoRow> infoRows = toInfoRows(infos, infoFont, textWidth);
        int infoLineCount = 0;
        for (InfoRow row : infoRows) {
            infoLineCount += row.lines()
                    .size();
        }

        double infoHeight = infoLineHeight * (infoLineCount - 1) + infoTextHeight;
        double textHeight = titleLineHeight * titleLines.size() + TITLE_GAP + infoHeight;
        double height = Math.max(ApprovalBox.height(style), textHeight);

        double titleBaseline = top + ReportText.ascent(titleFont);
        for (String line : titleLines) {
            canvas.text(line, titleFont, ReportAlign.LEFT, canvas.getLeft(), titleBaseline);
            titleBaseline += titleLineHeight;
        }

        double labelWidth = labelWidth(infoRows, infoFont);
        double infoBaseline = top + height - infoHeight + ReportText.ascent(infoFont);
        for (InfoRow row : infoRows) {
            canvas.text(row.label(), infoFont, ReportAlign.LEFT, canvas.getLeft(), infoBaseline);
            for (String line : row.lines()) {
                canvas.text(line, infoFont, ReportAlign.LEFT, canvas.getLeft() + labelWidth, infoBaseline);
                infoBaseline += infoLineHeight;
            }
        }

        if (approvalVisible) {
            ApprovalBox.draw(canvas, canvas.getRight(), top, approval.getTitles());
        }
        canvas.moveDown(height);
    }

    /**
     * 정보의 값은 가장 긴 이름 뒤의 폭에서 줄을 바꾼다.
     */
    private static List<InfoRow> toInfoRows(List<Info> infos, Font font, double textWidth) {
        double maxLabelWidth = 0;
        for (Info info : infos) {
            maxLabelWidth = Math.max(maxLabelWidth, ReportText.width(info.label(), font));
        }

        List<InfoRow> rows = new ArrayList<>();
        for (Info info : infos) {
            rows.add(new InfoRow(info.label(), ReportText.wrap(info.value(), font, textWidth - maxLabelWidth)));
        }
        return rows;
    }

    private static double labelWidth(List<InfoRow> rows, Font font) {
        double width = 0;
        for (InfoRow row : rows) {
            width = Math.max(width, ReportText.width(row.label(), font));
        }
        return width;
    }
}
