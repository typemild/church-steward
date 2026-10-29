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
 * UI 가이드라인의 Icon Button 구현체. 꺼진 동안에는 아이콘을 흐린 회색으로 그리고, 마우스를 올려도 손 모양 커서와 배경색을
 * 보이지 않는다(툴팁은 그대로 뜬다). FontIcon은 FlatLaf가 꺼진 아이콘을 회색으로 바꿔 주지 않으므로 꺼진 아이콘을 직접
 * 지정한다.
 */
public class IconButton extends JButton {
    private static final long serialVersionUID = 1L;

    private final int iconSize;
    private boolean hovered;

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

        setIcon(icon);
        setText(null);
        setOpaque(false);
        setBorderPainted(false);
        setContentAreaFilled(false);

        setFocusPainted(false);
        setPreferredSize(preferredSize);
        updateHoverState();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                updateHoverState();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                updateHoverState();
            }
        });
    }

    public void setIcon(FontAwesomeSolid icon) {
        setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
        setDisabledIcon(FontIcon.of(icon, iconSize, UIColors.ICON_DISABLED));
    }

    /**
     * 마우스를 올린 채로 꺼지거나 켜질 수 있으므로(누른 버튼이 바로 꺼지는 경우 등) 배경색과 커서를 함께 맞춘다.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateHoverState();
    }

    private void updateHoverState() {
        boolean enabled = isEnabled();
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        setContentAreaFilled(enabled && hovered);
        if (enabled && hovered) {
            setBackground(UIColors.SECONDARY_BACKGROUND_HOVER);
        }
    }

}
