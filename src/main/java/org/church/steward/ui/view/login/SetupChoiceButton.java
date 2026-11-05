package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.util.UIShapes;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 초기 설정 화면의 시작 방법 선택에 쓰는 카드 모양 버튼. 왼쪽에 아이콘, 가운데에 굵은 제목과 그 아래의 회색 설명, 오른쪽에 누르면
 * 넘어간다는 꺾쇠를 둔다.
 * <p>
 * 모양은 {@link org.church.steward.ui.component.panel.CardPanel}과 같은 흰 카드에 1px 회색
 * 테두리다. 테두리는 픽셀에 맞춰 1px로 그린다(두께가 픽셀에 맞지 않으면 변마다 진하기가 달라 카드가 눌려 들어간 것처럼 보인다).
 * <p>
 * 선택은 포커스로 나타낸다. 포커스를 받은 버튼이 선택된 것이고, 테두리와 꺾쇠가 Primary 색이 된다. 마우스를 올리면 그 버튼이
 * 포커스를 가져가므로, 여러 버튼 중 선택된 것은 늘 하나다. 다른 창이 떠서 잠시 포커스를 잃어도(일시적인 포커스 이동) 선택된 모양은
 * 그대로 둔다.
 */
class SetupChoiceButton extends JButton {

    private static final long serialVersionUID = 1L;

    private static final int PADDING_VERTICAL = 16;
    private static final int PADDING_HORIZONTAL = 18;
    private static final int ICON_GAP = 14;
    private static final int ICON_SIZE = 26;
    private static final int CHEVRON_SIZE = UIFonts.FONT_SIZE_BASE;

    private static final Font TITLE_FONT = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_LARGE);
    private static final Font DESCRIPTION_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private final JLabel chevronLabel = new JLabel();
    /** 선택된(마지막으로 포커스를 받은) 버튼인지 여부 */
    private boolean chosen = false;

    SetupChoiceButton(FontAwesomeSolid icon, String title, String description) {
        setLayout(new BorderLayout(ICON_GAP, 0));
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL,
                PADDING_HORIZONTAL));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 자식 레이블은 마우스 리스너가 없어 누름이 버튼으로 간다(JTextArea는 누름을 가로채므로 쓰지 않는다).
        JLabel iconLabel = new JLabel(FontIcon.of(icon, ICON_SIZE, UIColors.PRIMARY_BACKGROUND));
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(UIColors.TEXT_MAIN);

        JLabel descriptionLabel = new WrappingLabel(description);
        descriptionLabel.setFont(DESCRIPTION_FONT);
        descriptionLabel.setForeground(UIColors.TEXT_SUB);

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(descriptionLabel, BorderLayout.CENTER);

        add(iconLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        add(chevronLabel, BorderLayout.EAST);
        updateHighlight();

        // 마우스를 올리면 선택을 이 버튼으로 옮긴다. 마우스가 떠나도 선택은 그대로 둔다.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setChosen(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // 메시지 창 등으로 창 자체가 포커스를 잃은 경우에는 선택을 유지한다.
                if (!e.isTemporary()) {
                    setChosen(false);
                }
            }
        });
    }

    private void setChosen(boolean chosen) {
        this.chosen = chosen;
        updateHighlight();
    }

    private void updateHighlight() {
        chevronLabel.setIcon(FontIcon.of(FontAwesomeSolid.CHEVRON_RIGHT, CHEVRON_SIZE,
                chosen ? UIColors.PRIMARY_BACKGROUND : UIColors.ICON_DISABLED));
        repaint();
    }

    /**
     * 버튼 UI는 글자와 아이콘만으로 크기를 재므로, 안에 놓은 레이블들로 잰 크기를 쓴다. 설명이 줄바꿈되어 높아지면 버튼도 높아진다.
     */
    @Override
    public Dimension getPreferredSize() {
        return getLayout().preferredLayoutSize(this);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // CardPanel과 같이 그린다.
            int arc = UIShapes.CARD_ARC;
            g2.setColor(UIColors.CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(chosen ? UIColors.PRIMARY_BACKGROUND : UIColors.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    /**
     * 놓인 칸의 폭에 맞춰 줄바꿈하는 레이블.
     * {@link org.church.steward.ui.component.label.WrappingDescriptionLabel}과 같이 선호
     * 폭을 0으로 알려 버튼이 화면보다 넓어지지 않게 하고, 높이는 지금 폭에서 줄바꿈한 높이로 알린다. 그 레이블은 JTextArea라서
     * 누름을 가로채므로, 누름이 버튼으로 가도록 HTML을 쓰는 JLabel로 만든다.
     */
    private static class WrappingLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        WrappingLabel(String text) {
            // 넘긴 줄바꿈("\n")은 그대로 지킨다.
            super("<html>" + text.replace("\n", "<br>") + "</html>");
        }

        @Override
        public Dimension getPreferredSize() {
            View view = (View) getClientProperty(BasicHTML.propertyKey);
            Insets insets = getInsets();
            int width = getWidth() - insets.left - insets.right;
            if (view == null || width <= 0) {
                // 아직 배치되지 않았으면 한 줄 높이로 시작한다.
                return new Dimension(0, super.getPreferredSize().height);
            }
            view.setSize(width, 0);
            int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
            return new Dimension(0, height + insets.top + insets.bottom);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            boolean widthChanged = width != getWidth();
            super.setBounds(x, y, width, height);
            if (widthChanged) {
                // 폭이 바뀌면 줄 수가 달라지므로 높이를 다시 계산하게 한다.
                SwingUtilities.invokeLater(this::revalidate);
            }
        }
    }
}
