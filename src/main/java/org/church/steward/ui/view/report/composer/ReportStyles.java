package org.church.steward.ui.view.report.composer;

import java.awt.Font;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 여러 보고서가 함께 쓰는 글자 크기와 간격, 공통 요소.
 * 글자 크기는 기본 크기이며, 실제 크기는 작업대의 서식({@link ReportCanvas#getStyle()})이 정한다.
 */
final class ReportStyles {

    /**
     * 회계 연도 단위 보고서(예산안, 결산 보고서, 결산 세부 내역서)의 제목. 한 장에 담을 내용이 주간 보고서보다 적어 조금 크게 쓴다.
     */
    static final float ANNUAL_TITLE_SIZE = 13f;

    /**
     * 예산을 한 줄씩 나열하는 보고서(예산안, 결산 세부 내역서)의 글자 크기.
     * 행이 많지 않으므로 읽기 편하도록 보고서의 기본 크기보다 1pt씩 크게 쓴다.
     */
    static final float BUDGET_TABLE_SIZE = 9f;
    static final float BUDGET_TABLE_UNIT_SIZE = 8f;

    /** 제목과 그 아래 내용 사이의 간격 */
    static final double TITLE_GAP = 4;
    /** 절과 절 사이의 간격 */
    static final double SECTION_SPACING = 10;

    private static final double UNIT_GAP = 2;

    private ReportStyles() {
        // 인스턴스화 방지
    }

    /**
     * 현재 위치의 오른쪽 끝에 금액의 단위를 적고 그 아래로 이동한다.
     */
    static void drawUnit(ReportCanvas canvas, float baseSize) {
        Font font = canvas.getStyle()
                .regular(baseSize);
        canvas.text("(단위: 원)", font, ReportAlign.RIGHT, canvas.getRight(), canvas.getY() + ReportText.ascent(font),
                UIColors.REPORT_TEXT_SUB);
        canvas.moveDown(ReportText.lineHeight(font) + UNIT_GAP);
    }
}
