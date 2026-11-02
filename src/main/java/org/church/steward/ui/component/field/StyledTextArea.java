package org.church.steward.ui.component.field;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import com.formdev.flatlaf.FlatClientProperties;

public class StyledTextArea extends JTextArea {

    private static final long serialVersionUID = 1L;

    private final ComponentSize componentSize;

    public StyledTextArea(ComponentSize componentSize) {
        super();
        this.componentSize = componentSize;
        applyStyle(componentSize);
    }

    public StyledTextArea(int rows, int columns, ComponentSize componentSize) {
        super(rows, columns);
        this.componentSize = componentSize;
        applyStyle(componentSize);
    }

    /**
     * 입력할 수 있는 최대 글자 수를 정한다(줄바꿈도 한 글자로 센다). 넘치는 글자는 잘리고 입력란 아래에 안내가 잠깐 뜬다.
     */
    public void setMaxLength(int maxLength) {
        MaxLengthDocumentFilter.install(this, maxLength, new InputHintPopup(this, componentSize));
    }

    /**
     * 이 입력란을 감싸는 스크롤 패인을 만든다. 바깥 테두리는 스크롤 패인이 그리므로, 한 줄 입력란(TextComponent.arc)과 같은
     * 둥근 모서리를 지정한다. FlatLaf의 스크롤 패인은 기본이 네모난 모서리(ScrollPane.arc = 0)이고, 이것을 전체에 바꾸면
     * 목록 표나 보고서 뷰어의 스크롤 영역까지 둥글어지므로 여러 줄 입력란을 감쌀 때만 지정한다.
     */
    public JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.putClientProperty(FlatClientProperties.STYLE,
                "arc: " + UIManager.getInt("TextComponent.arc"));
        return scrollPane;
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
