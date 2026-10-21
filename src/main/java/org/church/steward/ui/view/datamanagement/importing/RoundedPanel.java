package org.church.steward.ui.view.datamanagement.importing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.church.steward.ui.util.UIColors;

/**
 * 흰 바탕에 테두리를 두른 둥근 모서리 사각형 영역. 가져오기 화면에서 진행 상황(제목과 단계)을 그 아래의 단계 화면과
 * 구분하는 데 쓴다.
 */
class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ARC = 12;
    private static final int PADDING_VERTICAL = 10;
    private static final int PADDING_HORIZONTAL = 14;

    RoundedPanel(LayoutManager layout) {
        super(layout);
        // 둥근 배경을 paintComponent에서 직접 그린다.
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL,
                PADDING_HORIZONTAL));
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

}
