package org.church.steward.ui.component.label;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class BadgeLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Font FONT_LARGE = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_LARGE);
    private static final Font FONT_BASE = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
    private static final Font FONT_SMALL = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private static final Dimension SIZE_LARGE = new Dimension(160, 36);
    private static final Dimension SIZE_MEDIUM = new Dimension(120, 28);
    private static final Dimension SIZE_SMALL = new Dimension(80, 20);

    private final Color foregroundColor;
    private final Color backgroundColor;

    public BadgeLabel(String text, ComponentSize componentSize) {
        this(null, text, UIColors.PRIMARY_TEXT, UIColors.PRIMARY_BACKGROUND, componentSize);
    }

    public BadgeLabel(FontAwesomeSolid icon, String text, ComponentSize componentSize) {
        this(icon, text, UIColors.PRIMARY_TEXT, UIColors.PRIMARY_BACKGROUND, componentSize);
    }

    public BadgeLabel(String text, Color foregroundColor, Color backgroundColor, ComponentSize componentSize) {
        this(null, text, foregroundColor, backgroundColor, componentSize);
    }

    public BadgeLabel(FontAwesomeSolid icon, String text, Color foregroundColor, Color backgroundColor,
            ComponentSize componentSize) {
        super(text);
        this.foregroundColor = foregroundColor;
        this.backgroundColor = backgroundColor;

        applyStyle(componentSize);

        if (icon != null) {
            applyIcon(icon);
        }
    }

    private void applyStyle(ComponentSize componentSize) {
        final Font font = switch (componentSize) {
        case LARGE -> FONT_LARGE;
        case MEDIUM -> FONT_BASE;
        case SMALL -> FONT_SMALL;
        };
        setFont(font);

        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);

        setForeground(foregroundColor);
        setOpaque(false); // 배경을 투명하게 만들고 paintComponent에서 직접 그리기

        final Dimension size = switch (componentSize) {
        case LARGE -> SIZE_LARGE;
        case MEDIUM -> SIZE_MEDIUM;
        case SMALL -> SIZE_SMALL;
        };
        setPreferredSize(size);
    }

    private void applyIcon(FontAwesomeSolid icon) {
        final int iconSize = Math.round(getFont().getSize() * 1.1f);
        setIcon(FontIcon.of(icon, iconSize, foregroundColor));
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(iconSize / 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

}
