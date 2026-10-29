package org.church.steward.ui.view.report.layout;

import java.awt.Font;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSettingConstraints;

/**
 * 보고서의 서식. 글꼴, 글자 크기 조정 단계, 행 간격을 정한다.
 * <p>
 * 글자 크기는 모든 글자에 같은 크기(한 단계에
 * {@link ReportSettingConstraints#FONT_SIZE_STEP_POINTS}pt)를 더하거나 빼서 제목과 본문의 크기
 * 차이를 그대로 둔다. 페이지 번호는 조정하지 않는다. 행 간격은 표의 행과 내역 줄처럼 줄이 이어지는 곳에서 글자 높이(1배)에 대한 한
 * 줄 높이의 비율이며, 늘어난 높이는 글자 위아래에 반씩 나눈다. 제목, 절 제목, 절 사이의 간격에는 적용하지 않는다.
 *
 * @param fontSizeStep       글자 크기 조정 단계. 0이 기본 크기다.
 * @param lineSpacingPercent 행 간격(%)
 */
public record ReportStyle(ReportFontFamily fontFamily, int fontSizeStep, int lineSpacingPercent) {

    public static final ReportStyle DEFAULT = new ReportStyle(ReportSettingConstraints.DEFAULT_FONT_FAMILY,
            ReportSettingConstraints.DEFAULT_FONT_SIZE_STEP, ReportSettingConstraints.DEFAULT_LINE_SPACING);

    /**
     * 기본 크기의 글자를 조정한 크기
     */
    public float size(float baseSize) {
        return baseSize + fontSizeStep * ReportSettingConstraints.FONT_SIZE_STEP_POINTS;
    }

    public Font regular(float baseSize) {
        return ReportFonts.regular(fontFamily, size(baseSize));
    }

    public Font bold(float baseSize) {
        return ReportFonts.bold(fontFamily, size(baseSize));
    }

    /**
     * 크기를 조정하지 않는 글자(페이지 번호)의 글꼴
     */
    public Font fixedRegular(float size) {
        return ReportFonts.regular(fontFamily, size);
    }

    /**
     * 행 간격을 적용한 한 줄의 높이
     */
    public double lineHeight(Font font) {
        return ReportText.lineHeight(font) * lineSpacingPercent / 100.0;
    }

    /**
     * 한 줄의 위쪽 끝에서 기준선까지의 거리. 늘어난 줄 높이를 글자 위아래에 반씩 나눈다.
     */
    public double baselineOffset(Font font) {
        return (lineHeight(font) - ReportText.lineHeight(font)) / 2 + ReportText.ascent(font);
    }

    public ReportStyle withFontFamily(ReportFontFamily fontFamily) {
        return new ReportStyle(fontFamily, fontSizeStep, lineSpacingPercent);
    }

    public ReportStyle withFontSizeStep(int fontSizeStep) {
        return new ReportStyle(fontFamily, fontSizeStep, lineSpacingPercent);
    }

    public ReportStyle withLineSpacing(int lineSpacingPercent) {
        return new ReportStyle(fontFamily, fontSizeStep, lineSpacingPercent);
    }

    /**
     * 가장 작은 글자의 기본 크기가 주어졌을 때, 그 글자가 허용
     * 범위({@link ReportSettingConstraints#SMALLEST_FONT_SIZE_MIN} ~
     * {@link ReportSettingConstraints#SMALLEST_FONT_SIZE_MAX})를 벗어나지 않는 가장 작은 조정 단계
     */
    public static int minFontSizeStep(float smallestBaseSize) {
        return (int) Math.ceil((ReportSettingConstraints.SMALLEST_FONT_SIZE_MIN - smallestBaseSize)
                / ReportSettingConstraints.FONT_SIZE_STEP_POINTS - 1e-6);
    }

    /**
     * {@link #minFontSizeStep}과 같은 기준의 가장 큰 조정 단계
     */
    public static int maxFontSizeStep(float smallestBaseSize) {
        return (int) Math.floor((ReportSettingConstraints.SMALLEST_FONT_SIZE_MAX - smallestBaseSize)
                / ReportSettingConstraints.FONT_SIZE_STEP_POINTS + 1e-6);
    }
}
