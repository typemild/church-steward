package org.church.steward.ui.component.button;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * UI 가이드라인의 Primary Button 구현체
 */
public class IconButton extends JButton {
    private static final long serialVersionUID = 1L;

    private final int iconSize;

    public IconButton(FontAwesomeSolid icon, ComponentSize componentSize) {
        super();

        iconSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };

        final int buttonSize = switch (componentSize) {
        case LARGE -> Math.round(iconSize * 2.4f);
        case MEDIUM -> iconSize * 2;
        case SMALL -> Math.round(iconSize * 1.6f);
        };
        final Dimension preferredSize = new Dimension(buttonSize, buttonSize);

        setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
        setText(null);
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        setPreferredSize(preferredSize);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setContentAreaFilled(true);
                setBackground(UIColors.SECONDARY_BACKGROUND_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setContentAreaFilled(false);
            }
        });
    }

    public void setIcon(FontAwesomeSolid icon) {
        setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
    }

}
