package org.church.steward.ui.component.field;

import java.awt.Font;

import javax.swing.JComboBox;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import com.formdev.flatlaf.FlatClientProperties;

public class StyledComboBox<E> extends JComboBox<E> {

    private static final long serialVersionUID = 1L;

    public StyledComboBox(ComponentSize componentSize) {
        super();

        applyStyle(componentSize);
    }

    public StyledComboBox(E[] items, ComponentSize componentSize) {
        super(items);

        applyStyle(componentSize);
    }

    private void applyStyle(ComponentSize componentSize) {
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        // FlatLaf 기본 안쪽 여백(2,6,2,6)에 크기별 여백을 더한다.
        final int verticalPadding = 2 + switch (componentSize) {
        case LARGE -> Math.round(fontSize * 0.27f);
        case MEDIUM -> Math.round(fontSize * 0.15f);
        case SMALL -> Math.round(fontSize * 0.09f);
        };
        final int horizontalPadding = 6;

        setFont(font);
        setBackground(UIColors.COMBO_BOX_BACKGROUND);
        // 테두리는 FlatLaf의 것을 그대로 쓴다(둥근 모서리, 포커스 색). 안쪽 여백만 스타일로 지정한다.
        putClientProperty(FlatClientProperties.STYLE, String.format("padding: %d,%d,%d,%d",
                verticalPadding, horizontalPadding, verticalPadding, horizontalPadding));
        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(this);
    }

}
