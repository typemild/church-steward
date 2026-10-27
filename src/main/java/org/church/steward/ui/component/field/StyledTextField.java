package org.church.steward.ui.component.field;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextField;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

public class StyledTextField extends JTextField {

    private static final long serialVersionUID = 1L;

    private final ComponentSize componentSize;

    public StyledTextField(ComponentSize componentSize) {
        super();
        this.componentSize = componentSize;
        applyStyle(componentSize);
    }

    public StyledTextField(int columns, ComponentSize componentSize) {
        super(columns);
        this.componentSize = componentSize;
        applyStyle(componentSize);
    }

    /**
     * 입력할 수 있는 최대 글자 수를 정한다. 넘치는 글자는 잘리고 입력란 아래에 안내가 잠깐 뜬다.
     */
    public void setMaxLength(int maxLength) {
        MaxLengthDocumentFilter.install(this, maxLength, new InputHintPopup(this, componentSize));
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
        // 테두리는 FlatLaf의 것을 그대로 쓴다(둥근 모서리, 포커스 색). 안쪽 여백만 margin으로 지정한다.
        setMargin(padding);
        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(this);
    }

}
