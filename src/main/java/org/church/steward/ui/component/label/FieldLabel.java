package org.church.steward.ui.component.label;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class FieldLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Font FONT_LARGE = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_LARGE);
    private static final Font FONT_BASE = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
    private static final Font FONT_SMALL = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    public FieldLabel(String text, int horizontalAlignment, ComponentSize componentSize) {
        this(text, horizontalAlignment, componentSize, false);
    }

    public FieldLabel(String text, ComponentSize componentSize) {
        this(text, componentSize, false);
    }

    public FieldLabel(String text, FontAwesomeSolid icon, int horizontalAlignment, ComponentSize componentSize) {
        this(text, icon, horizontalAlignment, componentSize, false);
    }

    public FieldLabel(String text, int horizontalAlignment, ComponentSize componentSize, boolean isBold) {
        super(text, horizontalAlignment);
        applyStyle(componentSize, isBold);
    }

    public FieldLabel(String text, ComponentSize componentSize, boolean isBold) {
        super(text);
        applyStyle(componentSize, isBold);
    }

    public FieldLabel(String text, FontAwesomeSolid icon, int horizontalAlignment, ComponentSize componentSize,
            boolean isBold) {
        super(text, horizontalAlignment);
        applyStyle(componentSize, isBold);
        applyIcon(List.of(icon));
    }

    /**
     * 아이콘 여러 개를 왼쪽부터 순서대로 붙여 하나의 아이콘으로 보여주는 레이블.
     * 아이콘 하나로 나타내기 어려운 기능을 두 아이콘의 조합으로 나타낼 때 쓴다(예: 글꼴 + 위아래 화살표 = 글자 크기).
     *
     * @param icons 왼쪽부터 붙일 아이콘. 하나 이상이어야 한다.
     */
    public FieldLabel(String text, List<FontAwesomeSolid> icons, int horizontalAlignment,
            ComponentSize componentSize) {
        super(text, horizontalAlignment);
        applyStyle(componentSize, false);
        applyIcon(icons);
    }

    private void applyStyle(ComponentSize componentSize, boolean isBold) {
        final Font baseFont = switch (componentSize) {
        case LARGE -> FONT_LARGE;
        case MEDIUM -> FONT_BASE;
        case SMALL -> FONT_SMALL;
        };
        final Font font = isBold ? baseFont.deriveFont(Font.BOLD) : baseFont.deriveFont(Font.PLAIN);
        setFont(font);
        setForeground(UIColors.TEXT_MAIN);
    }

    private void applyIcon(List<FontAwesomeSolid> icons) {
        final int iconSize = Math.round(getFont().getSize() * 1.15f);
        final List<Icon> fontIcons = icons.stream()
                .map(icon -> (Icon) FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN))
                .toList();
        setIcon(fontIcons.size() == 1 ? fontIcons.getFirst() : new HorizontalIcons(fontIcons));
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(iconSize / 2);
    }

    /**
     * 아이콘 여러 개를 가로로 붙여 하나의 아이콘으로 그린다. 세로는 가운데에 맞춘다.
     */
    private record HorizontalIcons(List<Icon> icons) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int height = getIconHeight();
            int left = x;
            for (Icon icon : icons) {
                icon.paintIcon(c, g, left, y + (height - icon.getIconHeight()) / 2);
                left += icon.getIconWidth();
            }
        }

        @Override
        public int getIconWidth() {
            int width = 0;
            for (Icon icon : icons) {
                width += icon.getIconWidth();
            }
            return width;
        }

        @Override
        public int getIconHeight() {
            int height = 0;
            for (Icon icon : icons) {
                height = Math.max(height, icon.getIconHeight());
            }
            return height;
        }
    }

}
