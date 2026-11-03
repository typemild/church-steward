package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.time.LocalDate;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 여러 보고서가 함께 쓰는 글자 크기와 간격, 공통 요소. 글자 크기는 기본 크기이며, 실제 크기는 작업대의
 * 서식({@link ReportCanvas#getStyle()})이 정한다.
 */
final class ReportStyles {

    /**
     * 회계 연도 단위 보고서(예산안, 결산 보고서, 결산 세부 내역서)의 제목. 한 장에 담을 내용이 주간 보고서보다 적어 조금 크게 쓴다.
     */
    static final float ANNUAL_TITLE_SIZE = 13f;

    /**
     * 예산을 한 줄씩 나열하는 보고서(예산안, 결산 세부 내역서)의 글자 크기. 행이 많지 않으므로 읽기 편하도록 보고서의 기본 크기보다
     * 1pt씩 크게 쓴다.
     */
    static final float BUDGET_TABLE_SIZE = 9f;
    static final float BUDGET_TABLE_UNIT_SIZE = 8f;

    /** 회계 연도 단위 보고서에서 제목 아래에 회계 연도의 기간을 적는 글자의 크기 */
    static final float SUBTITLE_SIZE = 9f;

    /** 제목과 그 아래 내용 사이의 간격 */
    static final double TITLE_GAP = 4;
    /** 절과 절 사이의 간격 */
    static final double SECTION_SPACING = 10;

    /** 제목과 회계 연도 기간 사이의 간격(기간 글자 높이에 대한 비율). 글자 크기를 키우면 간격도 함께 커진다. */
    private static final double SUBTITLE_GAP_RATIO = 0.5;
    private static final double UNIT_GAP = 2;

    private ReportStyles() {
        // 인스턴스화 방지
    }

    /**
     * 회계 연도 단위 보고서의 제목을 놓고, 그 아래 가운데에 "2026-01-01 ~ 2026-12-31"처럼 회계 연도의 기간을 적은 뒤
     * 제목 아래의 간격만큼 이동한다.
     */
    static void drawAnnualTitle(ReportCanvas canvas, String title, LocalDate startDate, LocalDate endDate) {
        canvas.title(title, ANNUAL_TITLE_SIZE);

        Font font = canvas.getStyle()
                .regular(SUBTITLE_SIZE);
        canvas.moveDown(ReportText.lineHeight(font) * SUBTITLE_GAP_RATIO);
        canvas.text(ReportFormats.period(startDate, endDate), font, ReportAlign.CENTER,
                (canvas.getLeft() + canvas.getRight()) / 2, canvas.getY() + ReportText.ascent(font));
        canvas.moveDown(ReportText.lineHeight(font) + TITLE_GAP);
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
