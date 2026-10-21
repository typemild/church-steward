package org.church.steward.ui.component.label;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class SubFieldLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Font FONT_LARGE = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_LARGE);
    private static final Font FONT_BASE = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
    private static final Font FONT_SMALL = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    public SubFieldLabel(String text, int horizontalAlignment, ComponentSize componentSize) {
        super(text, horizontalAlignment);
        applyStyle(componentSize);
    }

    public SubFieldLabel(String text, ComponentSize componentSize) {
        super(text);
        applyStyle(componentSize);
    }

    public SubFieldLabel(String text, FontAwesomeSolid icon, int horizontalAlignment, ComponentSize componentSize) {
        super(text, horizontalAlignment);
        applyStyle(componentSize);
        applyIcon(icon);
    }

    private void applyStyle(ComponentSize componentSize) {
        final Font font = switch (componentSize) {
        case LARGE -> FONT_LARGE;
        case MEDIUM -> FONT_BASE;
        case SMALL -> FONT_SMALL;
        };
        setFont(font);
        setForeground(UIColors.TEXT_SUB);
    }

    private void applyIcon(FontAwesomeSolid icon) {
        final int iconSize = Math.round(getFont().getSize() * 1.15f);
        setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(iconSize / 2);
    }

}
