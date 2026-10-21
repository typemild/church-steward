package org.church.steward.ui.component.field;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextArea;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

public class StyledTextArea extends JTextArea {

    private static final long serialVersionUID = 1L;

    public StyledTextArea(ComponentSize componentSize) {
        super();
        applyStyle(componentSize);
    }

    public StyledTextArea(int rows, int columns, ComponentSize componentSize) {
        super(rows, columns);
        applyStyle(componentSize);
    }

    private void applyStyle(ComponentSize componentSize) {
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Insets padding = switch (componentSize) {
        case LARGE -> new Insets(
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f));
        case MEDIUM -> new Insets(
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.46f),
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.46f));
        case SMALL -> new Insets(
                Math.round(fontSize * 0.2f),
                Math.round(fontSize * 0.27f),
                Math.round(fontSize * 0.2f),
                Math.round(fontSize * 0.27f));
        };

        setFont(font);
        setBackground(UIColors.TEXT_FIELD_BACKGROUND);
        // 바깥 테두리는 감싸는 JScrollPane에 FlatLaf가 그린다(입력란에 포커스가 있으면 색이 바뀐다).
        // 안쪽 여백만 margin으로 지정한다.
        setMargin(padding);
        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(this);

        setLineWrap(true);
        setWrapStyleWord(true);
    }

}
