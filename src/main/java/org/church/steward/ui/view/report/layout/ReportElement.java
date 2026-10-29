package org.church.steward.ui.view.report.layout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * 페이지 위의 정해진 자리에 그려지는 요소. 좌표는 페이지의 왼쪽 위를 원점으로 하는 포인트 단위다. PDF로 저장할 때는 요소를 종류별로
 * PDF 명령으로 옮기므로, 종류를 늘리면 PDF 쪽도 함께 고쳐야 한다.
 */
public sealed interface ReportElement {

    void paint(Graphics2D g);

    /**
     * 한 줄의 글자
     *
     * @param x        글자의 왼쪽 끝
     * @param baseline 글자의 기준선
     */
    record Text(String text, Font font, double x, double baseline, Color color) implements ReportElement {

        @Override
        public void paint(Graphics2D g) {
            g.setFont(font);
            g.setColor(color);
            g.drawString(text, (float) x, (float) baseline);
        }
    }

    /**
     * 직선
     */
    record Line(double x1, double y1, double x2, double y2, double width, Color color) implements ReportElement {

        @Override
        public void paint(Graphics2D g) {
            g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.setColor(color);
            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }

    /**
     * 사각형
     *
     * @param fill        채울 색. null이면 채우지 않는다.
     * @param stroke      테두리 색. null이면 테두리를 그리지 않는다.
     * @param strokeWidth 테두리 굵기
     */
    record Rect(double x, double y, double width, double height, Color fill, Color stroke, double strokeWidth)
            implements ReportElement {

        @Override
        public void paint(Graphics2D g) {
            Rectangle2D shape = new Rectangle2D.Double(x, y, width, height);
            if (fill != null) {
                g.setColor(fill);
                g.fill(shape);
            }
            if (stroke != null) {
                g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(stroke);
                g.draw(shape);
            }
        }
    }
}
