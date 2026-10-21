package org.church.steward.ui.component.button;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * UI 가이드라인의 Secondary Button 구현체
 */
public class SecondaryButton extends JButton {
    private static final long serialVersionUID = 1L;

    public SecondaryButton(String text, ComponentSize componentSize) {
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };

        final Font font = new Font(UIFonts.FONT_FAMILY, Font.BOLD, fontSize);
        final Insets padding = new Insets(
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.8f));

        this(text, font, padding);
    }

    private SecondaryButton(String text, Font font, Insets insets) {
        super(text);
        setFont(font);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setBackground(UIColors.SECONDARY_BACKGROUND);
        setForeground(UIColors.TEXT_MAIN);
        setOpaque(true);
        setContentAreaFilled(true);
        setBorderPainted(true);

        // FlatLaf 전용 프로퍼티를 사용하여 테두리 색상 지정
        putClientProperty("JButton.borderColor", UIColors.BORDER);

        // 내부 여백 설정
        setMargin(insets);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(UIColors.SECONDARY_BACKGROUND_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(UIColors.SECONDARY_BACKGROUND);
            }
        });
    }
}
