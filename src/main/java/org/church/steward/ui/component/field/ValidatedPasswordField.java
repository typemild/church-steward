package org.church.steward.ui.component.field;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.border.Border;
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

    private final ComponentSize componentSize;
    private Popup currentPopup;

    public ValidatedPasswordField(ComponentSize componentSize) {
        super();
        this.componentSize = componentSize;
        initialize(componentSize);
    }

    public ValidatedPasswordField(int columns, ComponentSize componentSize) {
        super(columns);
        this.componentSize = componentSize;
        initialize(componentSize);
    }

    public void setPlaceholder(String placeholder) {
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    }

    private void initialize(ComponentSize componentSize) {
        setupPasswordFilter();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideTooltip();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                hideTooltip();
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
                    showTooltip(MESSAGE);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches(PATTERN)) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    showTooltip(MESSAGE);
                }
            }
        });
    }

    private void showTooltip(String message) {
        hideTooltip();

        JPanel tipPanel = new JPanel(new BorderLayout());
        tipPanel.setBackground(new Color(255, 243, 230)); // 아주 연한 주황색
        tipPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 193, 7), 1)); // 따뜻한 노란색

        final int fontSize = switch (componentSize) {
        case LARGE -> Math.round(UIFonts.FONT_SIZE_LARGE * 0.9f);
        case MEDIUM -> Math.round(UIFonts.FONT_SIZE_BASE * 0.9f);
        case SMALL -> Math.round(UIFonts.FONT_SIZE_SMALL * 0.9f);
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Border padding = BorderFactory.createEmptyBorder(
                Math.round(fontSize * 0.38f),
                Math.round(fontSize * 0.76f),
                Math.round(fontSize * 0.38f),
                Math.round(fontSize * 0.76f));

        JLabel label = new JLabel(message);
        label.setForeground(new Color(139, 69, 19)); // 갈색 글자
        label.setBorder(padding);
        label.setFont(font);
        tipPanel.add(label, BorderLayout.CENTER);

        Point loc = getLocationOnScreen();
        currentPopup = PopupFactory.getSharedInstance()
                .getPopup(this, tipPanel, loc.x, loc.y + getHeight());
        currentPopup.show();
    }

    private void hideTooltip() {
        if (currentPopup != null) {
            currentPopup.hide();
            currentPopup = null;
        }
    }
}
