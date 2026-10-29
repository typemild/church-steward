package org.church.steward.ui.view.dashboard;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JLabel;

/**
 * 글자가 폭을 넘으면 글자 크기를 줄여서 한 줄에 다 보여주는 레이블. 금액처럼 길이를 예측할 수 없는 값을 크게 보여줄 때 쓴다. 최소
 * 크기로도 넘치면 말줄임표로 자른다.
 */
class AutoFitLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    private final int minFontSize;

    /**
     * @param font        폭이 충분할 때 쓰는 폰트
     * @param minFontSize 줄일 수 있는 가장 작은 글자 크기
     */
    AutoFitLabel(Font font, int minFontSize) {
        this.minFontSize = minFontSize;
        setFont(font);
    }

    @Override
    protected void paintComponent(Graphics g) {
        String text = getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        Graphics2D g2 = DashboardGraphics.create(g);
        try {
            Insets insets = getInsets();
            int availableWidth = getWidth() - insets.left - insets.right;
            int availableHeight = getHeight() - insets.top - insets.bottom;

            Font baseFont = getFont();
            FontMetrics baseMetrics = g2.getFontMetrics(baseFont);

            Font font = baseFont;
            FontMetrics metrics = baseMetrics;
            float size = baseFont.getSize2D();
            while (metrics.stringWidth(text) > availableWidth && size > minFontSize) {
                size -= 1f;
                font = baseFont.deriveFont(size);
                metrics = g2.getFontMetrics(font);
            }

            // 글자 크기가 줄어도 위아래로 움직이지 않게 기준선은 원래 폰트로 정한다.
            int baseline = insets.top + (availableHeight - baseMetrics.getHeight()) / 2 + baseMetrics.getAscent();

            g2.setFont(font);
            g2.setColor(getForeground());
            g2.drawString(DashboardGraphics.clip(text, metrics, availableWidth), insets.left, baseline);
        } finally {
            g2.dispose();
        }
    }
}
