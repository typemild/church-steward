package org.church.steward.ui.component.field;

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPasswordField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * 입력 문자 제한 및 한/영 전환 안내 툴팁 기능을 포함한 비밀번호 필드다.
 */
public class ValidatedPasswordField extends JPasswordField {

    private static final long serialVersionUID = 1L;
    private static final String PATTERN = "[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*";
    private static final String MESSAGE = "한/영 전환을 확인해 주세요.";

    private final InputHintPopup hintPopup;

    public ValidatedPasswordField(ComponentSize componentSize) {
        super();
        this.hintPopup = new InputHintPopup(this, componentSize);
        initialize(componentSize);
    }

    public ValidatedPasswordField(int columns, ComponentSize componentSize) {
        super(columns);
        this.hintPopup = new InputHintPopup(this, componentSize);
        initialize(componentSize);
    }

    public void setPlaceholder(String placeholder) {
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    }

    private void initialize(ComponentSize componentSize) {
        setupPasswordFilter();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                hintPopup.hide();
            }
        });

        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Insets padding = switch (componentSize) {
        case LARGE -> new Insets(
                (int) Math.round(fontSize * 0.8f),
                (int) Math.round(fontSize * 0.8f),
                (int) Math.round(fontSize * 0.8f),
                (int) Math.round(fontSize * 0.8f));
        case MEDIUM -> new Insets(
                (int) Math.round(fontSize * 0.46f),
                (int) Math.round(fontSize * 0.46f),
                (int) Math.round(fontSize * 0.46f),
                (int) Math.round(fontSize * 0.46f));
        case SMALL -> new Insets(
                (int) Math.round(fontSize * 0.27f),
                (int) Math.round(fontSize * 0.27f),
                (int) Math.round(fontSize * 0.27f),
                (int) Math.round(fontSize * 0.27f));
        };
        setFont(font);
        setBackground(UIColors.TEXT_FIELD_BACKGROUND);
        // 테두리는 FlatLaf의 것을 그대로 쓴다(둥근 모서리, 포커스 색). 안쪽 여백만 margin으로 지정한다.
        setMargin(padding);
        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(this);
    }

    private void setupPasswordFilter() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches(PATTERN)) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    hintPopup.show(MESSAGE);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches(PATTERN)) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    hintPopup.show(MESSAGE);
                }
            }
        });
    }
}
