package org.church.steward.ui.view.report.layout;

/**
 * 보고서 용지(A4 세로)의 크기와 여백. 단위는 포인트(1/72인치)이며 인쇄와 PDF가 같은 단위를 쓴다.
 */
public final class ReportPaper {

    private static final double POINTS_PER_MM = 72 / 25.4;

    public static final double WIDTH = 210 * POINTS_PER_MM;
    public static final double HEIGHT = 297 * POINTS_PER_MM;

    /** 상하좌우 여백. 대부분의 프린터가 인쇄할 수 있는 범위 안에서 가장 좁게 잡은 값이다. */
    public static final double MARGIN = 10 * POINTS_PER_MM;

    private ReportPaper() {
        // 인스턴스화 방지
    }

    /**
     * 밀리미터를 포인트로 바꾼다.
     */
    public static double mm(double millimeters) {
        return millimeters * POINTS_PER_MM;
    }
}
