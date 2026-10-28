package org.church.steward.domain.report.entity;

/**
 * 보고서 설정 값의 범위와 기본값. 서비스의 검증과 화면의 조정 버튼이 함께 쓴다.
 */
public final class ReportSettingConstraints {

    /** 글자 크기를 한 번 조정할 때 모든 글자에 더하거나 빼는 크기(pt) */
    public static final float FONT_SIZE_STEP_POINTS = 0.5f;
    /** 보고서에서 가장 작은 글자가 가질 수 있는 크기의 범위(pt) */
    public static final float SMALLEST_FONT_SIZE_MIN = 6f;
    public static final float SMALLEST_FONT_SIZE_MAX = 12f;
    /**
     * 글자 크기 조정 단계의 절댓값 상한. 실제 범위는 보고서마다 가장 작은 글자의 크기에 따라 화면이 정하므로, 서비스는 이 값으로
     * 터무니없는 값만 막는다.
     */
    public static final int FONT_SIZE_STEP_LIMIT = Math
            .round((SMALLEST_FONT_SIZE_MAX - SMALLEST_FONT_SIZE_MIN) / FONT_SIZE_STEP_POINTS);

    /** 행 간격(%). 글자 높이에 대한 한 줄 높이의 비율이다. */
    public static final int LINE_SPACING_MIN = 100;
    public static final int LINE_SPACING_MAX = 200;
    public static final int LINE_SPACING_STEP = 10;

    /** 미리보기의 배율(%) */
    public static final int ZOOM_MIN = 25;
    public static final int ZOOM_MAX = 400;
    public static final int ZOOM_STEP = 10;

    public static final ReportFontFamily DEFAULT_FONT_FAMILY = ReportFontFamily.NANUM_GOTHIC;
    public static final int DEFAULT_FONT_SIZE_STEP = 0;
    public static final int DEFAULT_LINE_SPACING = 130;
    public static final boolean DEFAULT_AUTO_FIT = false;
    public static final boolean DEFAULT_THUMBNAILS_VISIBLE = false;
    public static final ReportViewMode DEFAULT_VIEW_MODE = ReportViewMode.CONTINUOUS;
    public static final ReportZoomMode DEFAULT_ZOOM_MODE = ReportZoomMode.FIT_WIDTH;
    public static final int DEFAULT_ZOOM = 100;

    /** enum 칼럼의 길이 */
    public static final int REPORT_TYPE_MAX_LENGTH = 30;
    public static final int ENUM_MAX_LENGTH = 20;

    private ReportSettingConstraints() {
    }

}
