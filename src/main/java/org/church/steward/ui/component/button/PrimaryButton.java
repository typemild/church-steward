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
 * UI 가이드라인의 Primary Button 구현체
 */
public class PrimaryButton extends JButton {
    private static final long serialVersionUID = 1L;

    public PrimaryButton(String text, ComponentSize componentSize) {
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

    private PrimaryButton(String text, Font font, Insets insets) {
        super(text);
        setFont(font);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setBackground(UIColors.PRIMARY_BACKGROUND);
        setForeground(UIColors.PRIMARY_TEXT);
        setOpaque(true);
        setContentAreaFilled(true);
        setBorderPainted(false);

        // 내부 여백 설정
        setMargin(insets);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(UIColors.PRIMARY_BACKGROUND_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(UIColors.PRIMARY_BACKGROUND);
            }
        });
    }
}
