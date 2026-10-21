package org.church.steward.ui.util;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * 입력란의 포커스 테두리(FlatLaf가 포커스를 받은 입력란의 테두리를 포커스 색으로 그리는 것)를 켜고 끈다.
 * 포커스 테두리는 입력/수정 다이얼로그에서만 보인다. 공용 입력 컴포넌트는 만들 때 끄고, {@link DialogKeyHandler}가
 * 등록한 입력란만 켠다. 그래서 목록 화면의 검색란이나 사이드바의 회계 연도 선택란에는 포커스 테두리가 생기지 않는다.
 * FlatLaf가 테두리를 그릴 때 포커스 여부를 묻는
 * {@link FlatClientProperties#COMPONENT_FOCUS_OWNER}를 바꾸는 방식이다.
 * 표에는 쓰지 않는다. 표는 같은 값으로 선택 색(포커스가 없으면 비활성 색)도 정하기 때문이다.
 */
public final class FocusIndicator {

    private static final Predicate<JComponent> NEVER = _ -> false;

    /** 포커스가 이 컴포넌트나 그 하위 컴포넌트(관계자 입력란의 안쪽 입력 필드 등)에 있으면 포커스를 받은 것으로 본다. */
    private static final Predicate<JComponent> FOCUS_WITHIN = component -> {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getPermanentFocusOwner();
        return focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, component);
    };

    private FocusIndicator() {
        // 인스턴스화 방지
    }

    /**
     * 포커스 테두리를 보이게 한다. 여러 줄 입력란이면 감싼 스크롤 패인의 테두리에 나타난다.
     */
    public static void show(JComponent component) {
        component.putClientProperty(FlatClientProperties.COMPONENT_FOCUS_OWNER, FOCUS_WITHIN);
    }

    /**
     * 포커스 테두리를 숨긴다. 포커스는 그대로 받는다.
     */
    public static void hide(JComponent component) {
        component.putClientProperty(FlatClientProperties.COMPONENT_FOCUS_OWNER, NEVER);
    }
}
