package org.church.steward.ui.component.panel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.WrappingDescriptionLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIShapes;

/**
 * 둥근 흰색 카드. 수입/지출 관리의 총합 카드와 같은 모양이다. 설정 탭과 프로그램 소개 탭이 쓴다.
 * <p>
 * 항목을 나열할 때는 {@link #CardPanel()}으로 만들어 {@link #addRow}로 한 줄씩 놓는다. 한 줄은 왼쪽에 항목
 * 이름(오른쪽 정렬), 세로 구분선(조금 두껍고 양 끝이 둥근 선), 오른쪽에 값(입력란 등)과 그 아래의 설명이다. 줄 사이에는 가로
 * 구분선 없이 여백만 둔다. 줄의 높이는 값이 글자뿐이어도 한 줄 입력란 높이 이상이다. 모든 항목이 카드의 격자 하나를 함께 써서 이름
 * 칸의 폭이 가장 긴 이름에 맞춰지고 구분선과 값의 위치가 항목마다 같다. 내용 둘레의 여백(첫 항목 위, 마지막 항목 아래, 좌우)은 모두
 * 같다.
 * <p>
 * 항목 나열이 아닌 내용을 담을 때는 {@link #CardPanel(LayoutManager)}로 만들어 직접 배치한다.
 */
public class CardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ARC = UIShapes.CARD_ARC;
    /** 내용 둘레의 여백. 첫 항목 위, 마지막 항목 아래, 좌우가 모두 이 값이다. */
    private static final int OUTER_PADDING = 20;
    /** 한 항목의 위아래 여백. 항목 사이의 간격은 이 값의 두 배다. */
    private static final int ROW_PADDING = 7;
    /** 카드 자체의 위아래 안쪽 여백. 항목의 위아래 여백과 합쳐 OUTER_PADDING이 되게 한다. */
    private static final int PADDING_VERTICAL = OUTER_PADDING - ROW_PADDING;
    /** 세로 구분선 양옆의 간격 */
    private static final int NAME_GAP = 16;
    /** 세로 구분선의 두께. 끝은 둥글게 그린다. */
    private static final int DIVIDER_THICKNESS = 2;
    /** 값과 설명 사이의 간격 */
    private static final int DESCRIPTION_GAP = 8;
    /**
     * 한 항목의 최소 높이(설명 제외). 한 줄 입력란의 높이다. 값이 글자뿐인 줄(다운로드 링크, 사용자 ID)이 버튼이나 입력란이 있는
     * 줄보다 낮아 보이지 않게 모든 줄을 이 높이 이상으로 맞춘다.
     */
    private static final int ROW_MIN_HEIGHT = new StyledTextField(ComponentSize.MEDIUM).getPreferredSize().height;

    /** 다음 항목을 놓을 격자의 행 */
    private int nextGridY = 0;

    /**
     * 항목을 {@link #addRow}로 나열하는 카드를 만든다.
     */
    public CardPanel() {
        this(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, OUTER_PADDING, PADDING_VERTICAL, OUTER_PADDING));
    }

    /**
     * 내용을 직접 배치하는 카드를 만든다. 안쪽 여백은 호출하는 쪽이 정한다.
     */
    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
    }

    /**
     * 항목 한 줄을 더한다. 항목 이름과 내용 사이에는 세로 구분선을 긋는다. {@link #CardPanel()}로 만든 카드에서만 쓴다.
     *
     * @param name             항목 이름
     * @param control          값을 보여 주거나 고르는 컴포넌트(버튼 등을 함께 담은 패널일 수 있다). 늘이지 않고 제
     *                         폭대로 둔다.
     * @param descriptionLines 값 아래에 적을 설명. 줄마다 하나씩 넘긴다.
     */
    public void addRow(String name, JComponent control, String... descriptionLines) {
        boolean hasDescription = descriptionLines.length > 0;
        GridBagConstraints gbc = new GridBagConstraints();
        int firstGridY = nextGridY;

        // 항목 이름은 구분선 쪽(오른쪽)으로 붙이고, 값 높이의 가운데에 맞춘다. 이름 칸의 폭은 가장 긴 이름에 맞춰진다.
        // 이름의 높이를 한 줄 입력란 높이 이상으로 늘려(글자는 가운데에 그대로 있다) 줄마다 높이를 맞춘다.
        FieldLabel nameLabel = new FieldLabel(name, ComponentSize.MEDIUM, true);
        gbc.gridx = 0;
        gbc.gridy = firstGridY;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.ipady = Math.max(0, ROW_MIN_HEIGHT - nameLabel.getPreferredSize().height);
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        add(nameLabel, gbc);

        // 항목 이름과 내용 사이의 세로 구분선. 값부터 설명 끝까지 내용의 높이만큼 긋는다.
        gbc.gridx = 1;
        gbc.gridheight = hasDescription ? 2 : 1;
        gbc.ipady = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(ROW_PADDING, NAME_GAP, ROW_PADDING, NAME_GAP);
        add(new RoundedDivider(), gbc);

        gbc.gridx = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        add(control, gbc);

        if (hasDescription) {
            // 설명은 칸의 폭에 맞춰 줄바꿈한다. 폭을 늘이지 않으므로 카드의 폭은 값과 이름으로만 정해진다.
            gbc.gridy = firstGridY + 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(DESCRIPTION_GAP, 0, ROW_PADDING, 0);
            add(new WrappingDescriptionLabel(String.join("\n", descriptionLines), ComponentSize.SMALL), gbc);
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
     * 항목 이름과 내용 사이의 세로 구분선.
     */
    private static class RoundedDivider extends JComponent {

        private static final long serialVersionUID = 1L;

        RoundedDivider() {
            Dimension size = new Dimension(DIVIDER_THICKNESS, 0);
            setPreferredSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIColors.BORDER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getWidth(), getWidth());
            g2.dispose();
        }
    }
}
