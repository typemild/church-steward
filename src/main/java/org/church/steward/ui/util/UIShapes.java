package org.church.steward.ui.util;

/**
 * 화면에서 직접 그리는 모양의 공용 값.
 */
public final class UIShapes {

    /**
     * 다른 요소를 담는 둥근 상자(카드, 주의사항 상자 등)의 모서리 둥글기. {@code fillRoundRect}의 arc 값(지름)이므로
     * 반지름은 이 값의 절반이다. 입력란과 버튼(FlatLaf의 {@code Component.arc} 5, {@code Button.arc}
     * 6)보다 조금 더 둥글다.
     */
    public static final int CARD_ARC = 8;

    private UIShapes() {
    }
}
