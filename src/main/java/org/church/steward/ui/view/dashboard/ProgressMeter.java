package org.church.steward.ui.view.dashboard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

import org.church.steward.ui.util.UIColors;

/**
 * 예산 대비 진행률을 보여주는 가로 막대. 100%를 넘는 값은 가득 찬 막대로 그린다.
 */
class ProgressMeter extends JComponent {

    private static final long serialVersionUID = 1L;

    static final int THICKNESS = 6;

    private final Color fillColor;
    private double ratio;

    ProgressMeter(Color fillColor) {
        this.fillColor = fillColor;
        setOpaque(false);
        setPreferredSize(new Dimension(40, THICKNESS));
    }

    /**
     * @param ratio 진행률(1.0이 100%)
     */
    void setRatio(double ratio) {
        this.ratio = ratio;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = DashboardGraphics.create(g);
        paintMeter(g2, 0, (getHeight() - THICKNESS) / 2, getWidth(), ratio, fillColor);
        g2.dispose();
    }

    /**
     * 지정한 위치에 진행 막대를 그린다. 다른 컴포넌트가 자기 영역 안에 막대를 그릴 때도 쓴다.
     */
    static void paintMeter(Graphics2D g2, int x, int y, int width, double ratio, Color fillColor) {
        g2.setColor(UIColors.METER_TRACK);
        g2.fillRoundRect(x, y, width, THICKNESS, THICKNESS, THICKNESS);

        double clamped = Math.max(0, Math.min(1, ratio));
        if (clamped <= 0) {
            return;
        }
        // 값이 아주 작아도 채워진 부분이 보이도록 최소 폭을 둔다.
        int fillWidth = Math.max(THICKNESS, (int) Math.round(width * clamped));
        g2.setColor(fillColor);
        g2.fillRoundRect(x, y, Math.min(fillWidth, width), THICKNESS, THICKNESS, THICKNESS);
    }
}
