package org.church.steward.ui.view.settings;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.View;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.util.UIShapes;

/**
 * 설정 탭의 항목을 담는 둥근 흰색 카드. 수입/지출 관리의 총합 카드와 같은 모양이다.
 * <p>
 * 항목은 {@link #addRow}로 한 줄씩 놓는다. 한 줄은 왼쪽에 항목 이름(오른쪽 정렬), 세로 구분선, 오른쪽에 입력란과 그
 * 아래의 설명이다. 줄 사이에는 가로 구분선 없이 여백만 둔다. 모든 항목이 카드의 격자 하나를 함께 써서 이름 칸의 폭이 가장 긴 이름에
 * 맞춰지고 구분선과 입력란의 위치가 항목마다 같다. 카드의 좌우 안쪽 여백은 세로 구분선 양옆 간격과 같아서, 가장 긴 이름의 좌우 여백이
 * 같다.
 */
class SettingsCardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ARC = UIShapes.CARD_ARC;
    private static final int PADDING_VERTICAL = 6;
    /** 세로 구분선 양옆의 간격. 카드의 좌우 안쪽 여백도 이 값이다. */
    private static final int NAME_GAP = 16;
    /** 한 항목의 위아래 여백 */
    private static final int ROW_PADDING = 14;
    /** 입력란과 설명 사이의 간격 */
    private static final int DESCRIPTION_GAP = 8;

    /** 다음 항목을 놓을 격자의 행 */
    private int nextGridY = 0;

    SettingsCardPanel() {
        super(new GridBagLayout());
        setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, NAME_GAP, PADDING_VERTICAL, NAME_GAP));
    }

    /**
     * 항목 한 줄을 더한다. 항목 이름과 내용 사이에는 세로 구분선을 긋는다.
     *
     * @param name             항목 이름
     * @param control          값을 보여 주거나 고르는 컴포넌트(저장 완료 표시 등을 함께 담은 패널일 수 있다). 늘이지
     *                         않고 제 폭대로 둔다.
     * @param descriptionLines 입력란 아래에 적을 설명. 줄마다 하나씩 넘긴다.
     */
    void addRow(String name, JComponent control, String... descriptionLines) {
        boolean hasDescription = descriptionLines.length > 0;
        GridBagConstraints gbc = new GridBagConstraints();
        int firstGridY = nextGridY;

        // 항목 이름은 구분선 쪽(오른쪽)으로 붙이고, 입력란 높이의 가운데에 맞춘다. 이름 칸의 폭은 가장 긴 이름에 맞춰진다.
        FieldLabel nameLabel = new FieldLabel(name, ComponentSize.MEDIUM, true);
        gbc.gridx = 0;
        gbc.gridy = firstGridY;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        add(nameLabel, gbc);

        // 항목 이름과 내용 사이의 세로 구분선. 입력란부터 설명 끝까지 내용의 높이만큼 긋는다.
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(UIColors.BORDER);
        gbc.gridx = 1;
        gbc.gridheight = hasDescription ? 2 : 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(ROW_PADDING, NAME_GAP, ROW_PADDING, NAME_GAP);
        add(separator, gbc);

        gbc.gridx = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        add(control, gbc);

        if (hasDescription) {
            // 설명은 칸의 폭에 맞춰 줄바꿈한다. 폭을 늘이지 않으므로 카드의 폭은 입력란과 이름으로만 정해진다.
            gbc.gridy = firstGridY + 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(DESCRIPTION_GAP, 0, ROW_PADDING, 0);
            add(new DescriptionText(String.join("\n", descriptionLines)), gbc);
        }

        nextGridY = firstGridY + (hasDescription ? 2 : 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIColors.CARD_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.setColor(UIColors.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * 칸의 폭에 맞춰 줄바꿈하는 회색 설명. 넘긴 줄바꿈("\n")은 그대로 지키고, 칸보다 긴 줄은 단어 단위로 나눈다.
     * <p>
     * 선호 폭을 0으로 알려 설명이 카드를 넓히지 않게 한다. 설명이 칸보다 넓다고 알리면 카드의 선호 폭이 화면보다 넓어지고, 그러면
     * {@link GridBagLayout}은 모든 컴포넌트를 최소 크기로 배치해 입력란과 세로 구분선이 거의 보이지 않게 된다. 높이는 지금
     * 폭에서 줄바꿈한 높이이며, 폭이 바뀌면 높이를 다시 계산하도록 레이아웃을 다시 한다.
     */
    private static class DescriptionText extends JTextArea {

        private static final long serialVersionUID = 1L;

        private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

        DescriptionText(String text) {
            super(text);
            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setBorder(null);
            setFont(FONT);
            setForeground(UIColors.TEXT_SUB);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            int width = getWidth() - insets.left - insets.right;
            int height;
            if (width > 0) {
                View root = getUI().getRootView(this);
                root.setSize(width, Integer.MAX_VALUE);
                height = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS));
            } else {
                // 아직 배치되지 않았으면 넘긴 줄 수만큼의 높이로 시작한다.
                height = getLineCount() * getFontMetrics(getFont()).getHeight();
            }
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
                SwingUtilities.invokeLater(this::revalidate);
            }
        }
    }
}
