package org.church.steward.ui.view.report;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;

/**
 * 보고서 미리보기. 쪽들을 세로로 늘어놓고 화면 폭에 맞춰 축소하거나 확대해 보여준다.
 * 인쇄나 PDF와 같은 {@link ReportDocument}를 그대로 그리므로 줄바꿈과 쪽 나눔이 실제 출력과 같다.
 */
public class ReportPreviewPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    /** 쪽과 쪽 사이, 쪽과 화면 가장자리 사이의 간격 */
    private static final int PAGE_GAP = 12;
    private static final int UNIT_INCREMENT = 24;
    private static final int FALLBACK_WIDTH = 600;

    private transient ReportDocument document;

    public ReportPreviewPanel() {
        setBackground(UIColors.REPORT_PREVIEW_BACKGROUND);
    }

    /**
     * 보여줄 보고서를 바꾼다. null이면 아무것도 보여주지 않는다.
     */
    public void setDocument(ReportDocument document) {
        this.document = document;
        revalidate();
        repaint();
    }

    /**
     * 용지의 폭이 주어진 화면 폭에 꽉 차도록 하는 배율
     */
    private static double scaleFor(int width) {
        return Math.max(0.1, (width - PAGE_GAP * 2) / ReportPaper.WIDTH);
    }

    /**
     * 높이는 폭에 맞춘 배율에 따라 달라지므로 스크롤 영역의 폭을 기준으로 계산한다.
     */
    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        int width = parent != null && parent.getWidth() > 0 ? parent.getWidth() : FALLBACK_WIDTH;
        if (document == null) {
            return new Dimension(width, 0);
        }

        double pageHeight = ReportPaper.HEIGHT * scaleFor(width);
        int height = (int) Math.ceil(PAGE_GAP + document.getPageCount() * (pageHeight + PAGE_GAP));
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (document == null) {
            return;
        }

        double scale = scaleFor(getWidth());
        double pageWidth = ReportPaper.WIDTH * scale;
        double pageHeight = ReportPaper.HEIGHT * scale;
        Rectangle clip = g.getClipBounds();

        for (int i = 0; i < document.getPageCount(); i++) {
            double top = PAGE_GAP + i * (pageHeight + PAGE_GAP);
            if (clip != null && (top + pageHeight < clip.y || top > clip.y + clip.height)) {
                continue;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(PAGE_GAP, top);
                g2.setColor(UIColors.REPORT_PAPER);
                g2.fillRect(0, 0, (int) Math.round(pageWidth), (int) Math.round(pageHeight));
                g2.setColor(UIColors.BORDER);
                g2.drawRect(0, 0, (int) Math.round(pageWidth), (int) Math.round(pageHeight));

                g2.scale(scale, scale);
                document.paintPage(g2, i);
            } finally {
                g2.dispose();
            }
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
