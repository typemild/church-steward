package org.church.steward.ui.view.report.viewer;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;

/**
 * 미리보기 왼쪽의 페이지 목록. 모든 페이지를 작게 줄여 위에서 아래로 늘어놓고, 페이지 번호를 그 아래에 적는다.
 * 페이지 그림은 그릴 때마다 문서에서 바로 그리므로 따로 보관하지 않는다(HiDPI 화면에서도 흐려지지 않는다).
 */
class ReportThumbnailList extends JList<Integer> {

    private static final long serialVersionUID = 1L;

    /** 줄인 페이지의 폭 */
    static final int THUMBNAIL_WIDTH = 90;
    private static final int THUMBNAIL_HEIGHT = (int) Math.round(THUMBNAIL_WIDTH * ReportPaper.HEIGHT
            / ReportPaper.WIDTH);
    private static final int PADDING = 8;
    private static final int LABEL_GAP = 4;
    private static final Font LABEL_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private final DefaultListModel<Integer> model = new DefaultListModel<>();
    private transient ReportDocument document;

    ReportThumbnailList() {
        setModel(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBackground(UIColors.BACKGROUND);
        setCellRenderer(new ThumbnailRenderer());

        FontMetrics metrics = getFontMetrics(LABEL_FONT);
        setFixedCellWidth(THUMBNAIL_WIDTH + PADDING * 2);
        setFixedCellHeight(PADDING + THUMBNAIL_HEIGHT + LABEL_GAP + metrics.getHeight() + PADDING / 2);
    }

    /**
     * 보여줄 문서를 바꾼다. 페이지 수만큼 항목을 만든다.
     */
    void setDocument(ReportDocument document) {
        this.document = document;
        model.clear();
        int count = document == null ? 0 : document.getPageCount();
        for (int i = 0; i < count; i++) {
            model.addElement(i);
        }
    }

    /**
     * 페이지 하나를 그리는 칸
     */
    private final class ThumbnailRenderer extends JComponent implements ListCellRenderer<Integer> {

        private static final long serialVersionUID = 1L;

        private int page;
        private boolean selected;

        @Override
        public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index,
                boolean isSelected, boolean cellHasFocus) {
            this.page = value;
            this.selected = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFixedCellWidth(), getFixedCellHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(UIColors.BACKGROUND);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int left = (getWidth() - THUMBNAIL_WIDTH) / 2;
                int top = PADDING;
                g2.setColor(UIColors.REPORT_PAPER);
                g2.fillRect(left, top, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

                if (document != null && page < document.getPageCount()) {
                    Graphics2D pageGraphics = (Graphics2D) g2.create();
                    try {
                        pageGraphics.clipRect(left, top, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
                        pageGraphics.translate(left, top);
                        double scale = THUMBNAIL_WIDTH / ReportPaper.WIDTH;
                        pageGraphics.scale(scale, scale);
                        document.paintPage(pageGraphics, page);
                    } finally {
                        pageGraphics.dispose();
                    }
                }

                // 고른 페이지는 굵은 Primary 색 테두리로, 나머지는 옅은 테두리로 감싼다.
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (selected) {
                    g2.setColor(UIColors.PRIMARY_BACKGROUND);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRect(left - 1, top - 1, THUMBNAIL_WIDTH + 1, THUMBNAIL_HEIGHT + 1);
                } else {
                    g2.setColor(UIColors.BORDER);
                    g2.drawRect(left, top, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);
                }

                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(LABEL_FONT);
                g2.setColor(selected ? UIColors.TEXT_HIGHLIGHT : UIColors.TEXT_MAIN);
                FontMetrics metrics = g2.getFontMetrics();
                String label = String.valueOf(page + 1);
                g2.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
                        top + THUMBNAIL_HEIGHT + LABEL_GAP + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
