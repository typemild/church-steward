package org.church.steward.ui.component.button;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 입력란 바로 옆에 붙는 아이콘 버튼(날짜 피커의 달력 버튼, 예산 선택 버튼). 툴바의 {@link IconButton}과 달리 입력란과
 * 한 덩어리로 보이도록 테두리와 흰 배경을 두고, 입력란 높이에 맞춘 정사각형으로 만든다. 꺼진 동안에는 아이콘을 흐린 회색으로 그리고,
 * 마우스를 올려도 손 모양 커서와 배경색을 보이지 않는다(툴팁은 그대로 뜬다). 라이브러리가 만든 버튼(LGoodDatePicker의 달력
 * 버튼)은 클래스를 바꿀 수 없으므로 {@link #decorate}로 같은 모양을 입힌다.
 */
public class FieldButton extends JButton {
    private static final long serialVersionUID = 1L;

    /**
     * @param field 버튼이 붙는 입력란. 이 입력란의 높이로 버튼의 크기를 정한다.
     */
    public FieldButton(FontAwesomeSolid icon, ComponentSize componentSize, JComponent field) {
        super();
        decorate(this, icon, componentSize, field);
    }

    /**
     * 이미 만들어진 버튼을 입력란에 붙는 버튼의 모양으로 꾸민다.
     *
     * @param field 버튼이 붙는 입력란. 이 입력란의 높이로 버튼의 크기를 정하므로 글꼴과 여백이 정해진 뒤에 호출한다.
     */
    public static void decorate(JButton button, FontAwesomeSolid icon, ComponentSize componentSize, JComponent field) {
        final int iconSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };

        button.setText(null);
        button.setIcon(FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN));
        // FontIcon은 FlatLaf가 꺼진 아이콘을 회색으로 바꿔 주지 않으므로 직접 지정한다.
        button.setDisabledIcon(FontIcon.of(icon, iconSize, UIColors.ICON_DISABLED));
        button.setFocusPainted(false);
        button.setBackground(UIColors.SECONDARY_BACKGROUND);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        // FlatLaf 전용 프로퍼티를 사용하여 테두리 색상 지정
        button.putClientProperty("JButton.borderColor", UIColors.BORDER);
        button.setMargin(new Insets(0, 0, 0, 0));

        final int size = field.getPreferredSize().height;
        final Dimension squareSize = new Dimension(size, size);
        button.setPreferredSize(squareSize);
        button.setMinimumSize(squareSize);

        HoverHandler hoverHandler = new HoverHandler(button);
        button.addMouseListener(hoverHandler);
        button.addPropertyChangeListener("enabled", hoverHandler);
        hoverHandler.update();
    }

    /**
     * 마우스를 올렸을 때의 배경색과 커서를 켜짐 여부에 맞춘다. 마우스를 올린 채로 꺼지거나 켜질 수 있으므로 켜짐 여부가 바뀔 때도 다시
     * 맞춘다.
     */
    private static final class HoverHandler extends MouseAdapter implements PropertyChangeListener {

        private final JButton button;
        private boolean hovered;

        HoverHandler(JButton button) {
            this.button = button;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            hovered = true;
            update();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hovered = false;
            update();
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            update();
        }

        void update() {
            boolean enabled = button.isEnabled();
            button.setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            button.setBackground(enabled && hovered
                    ? UIColors.SECONDARY_BACKGROUND_HOVER
                    : UIColors.SECONDARY_BACKGROUND);
        }
    }
}
