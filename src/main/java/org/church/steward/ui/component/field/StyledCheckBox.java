package org.church.steward.ui.component.field;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.border.Border;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIFonts;

public class StyledCheckBox extends JCheckBox {

    private static final long serialVersionUID = 1L;

    public StyledCheckBox(String text, ComponentSize componentSize) {
        super(text);

        applyStyle(componentSize);
    }

    private void applyStyle(ComponentSize componentSize) {
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Border padding = switch (componentSize) {
        case LARGE -> BorderFactory.createEmptyBorder(
                Math.round(fontSize * 0.27f),
                Math.round(fontSize * 0.2f),
                Math.round(fontSize * 0.27f),
                Math.round(fontSize * 0.2f));
        case MEDIUM -> BorderFactory.createEmptyBorder(
                Math.round(fontSize * 0.15f),
                Math.round(fontSize * 0.1f),
                Math.round(fontSize * 0.15f),
                Math.round(fontSize * 0.1f));
        case SMALL -> BorderFactory.createEmptyBorder(
                Math.round(fontSize * 0.09f),
                Math.round(fontSize * 0.04f),
                Math.round(fontSize * 0.09f),
                Math.round(fontSize * 0.04f));
        };

        setFont(font);
        setBorder(padding);
    }

}
