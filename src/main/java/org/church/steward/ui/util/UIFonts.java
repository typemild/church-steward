package org.church.steward.ui.util;

/**
 * 🎨 청지기(Steward) UI 디자인 가이드라인의 타이포그래피를 정의한 유틸리티 클래스
 */
public final class UIFonts {

    private UIFonts() {
        // 인스턴스화 방지
    }

    public static final String FONT_FAMILY = "SansSerif";

    public static final int FONT_SIZE_BASE = 13;
    public static final int FONT_SIZE_SMALL = Math.round(FONT_SIZE_BASE * 0.85f);
    public static final int FONT_SIZE_LARGE = Math.round(FONT_SIZE_BASE * 1.15f);
    public static final int FONT_SIZE_EXTRA_LARGE = Math.round(FONT_SIZE_BASE * 1.7f);

}
