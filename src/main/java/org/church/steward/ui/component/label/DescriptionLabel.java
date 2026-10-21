package org.church.steward.ui.component.label;

import java.awt.Font;

import javax.swing.JTextArea;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

public class DescriptionLabel extends JTextArea {

    private static final long serialVersionUID = 1L;

    private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);

    public DescriptionLabel(String text) {
        super(text);
        applyStyle();
    }

    private void applyStyle() {
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setFont(FONT);
        setForeground(UIColors.TEXT_MAIN);
    }

}
