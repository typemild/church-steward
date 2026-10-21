package org.church.steward.ui.view.dashboard;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.church.steward.ui.util.UIColors;

/**
 * 대시보드의 각 영역을 담는 둥근 카드
 */
class DashboardCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ARC = 12;
    private static final int PADDING_VERTICAL = 12;
    private static final int PADDING_HORIZONTAL = 14;

    DashboardCard(LayoutManager layout) {
        super(layout);
        setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL,
                PADDING_HORIZONTAL));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = DashboardGraphics.create(g);
        g2.setColor(UIColors.CARD_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.setColor(UIColors.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}
