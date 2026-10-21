package org.church.steward.ui.component.label;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class SectionHeaderLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_LARGE);

    public SectionHeaderLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        applyStyle();
    }

    public SectionHeaderLabel(String text) {
        super(text);
        applyStyle();
    }

    public SectionHeaderLabel(String text, FontAwesomeSolid icon, int horizontalAlignment) {
        super(text, horizontalAlignment);
        applyStyle();
        applyIcon(icon);
    }

    private void applyStyle() {
        setFont(FONT);
        setForeground(UIColors.TEXT_MAIN);
    }

    private void applyIcon(FontAwesomeSolid icon) {
        final int iconSize = Math.round(getFont().getSize() * 1.15f);
        setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(iconSize / 2);
    }
}
