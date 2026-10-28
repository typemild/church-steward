package org.church.steward.ui.view.report.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;

/**
 * 보고서 미리보기의 페이지 영역. 이어 보기에서는 페이지들을 세로로 늘어놓고, 페이지별 보기에서는 현재 페이지 하나만 보여준다.
 * 인쇄나 PDF와 같은 {@link ReportDocument}를 그대로 그리므로 줄바꿈과 페이지 나눔이 실제 출력과 같다.
 * <p>
 * 배율은 폭 맞춤, 페이지 맞춤, 직접 지정 중 하나로 정한다. 100%는 실제 크기(화면의 1인치를 96픽셀로 보고 1pt를 1/72인치로
 * 그린 크기)다.
 * 페이지가 미리보기 영역보다 좁으면 가운데에 놓고, 넓으면 가로 스크롤이 생긴다.
 */
class ReportPreviewPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    /** 페이지와 페이지 사이, 페이지와 화면 가장자리 사이의 간격 */
    static final int PAGE_GAP = 12;
    /** 100%일 때 1pt가 차지하는 화면 픽셀. HiDPI 화면의 배율은 Swing이 따로 적용한다. */
    static final double PIXELS_PER_POINT = 96.0 / 72.0;

    private static final int UNIT_INCREMENT = 24;
    private static final Dimension FALLBACK_EXTENT = new Dimension(600, 600);

    /**
     * 페이지 안의 한 점. 배율을 바꾼 뒤에도 같은 내용이 같은 자리에 보이도록 스크롤 위치를 맞추는 데 쓴다.
     *
     * @param page 페이지 순번(0부터)
     * @param x    페이지의 왼쪽 끝에서의 거리(pt)
     * @param y    페이지의 위쪽 끝에서의 거리(pt)
     */
    record PagePoint(int page, double x, double y) {
    }

    private transient ReportDocument document;
    private ReportViewMode viewMode = ReportSettingConstraints.DEFAULT_VIEW_MODE;
    private ReportZoomMode zoomMode = ReportSettingConstraints.DEFAULT_ZOOM_MODE;
    private int zoomPercent = ReportSettingConstraints.DEFAULT_ZOOM;
    /** 페이지별 보기에서 보여주는 페이지 */
    private int currentPage;

    ReportPreviewPanel() {
        setBackground(UIColors.REPORT_PREVIEW_BACKGROUND);
    }

    ReportDocument getDocument() {
        return document;
    }

    int getPageCount() {
        return document == null ? 0 : document.getPageCount();
    }

    /**
     * 보여줄 보고서를 바꾼다. null이면 아무것도 보여주지 않는다.
     */
    void setDocument(ReportDocument document) {
        this.document = document;
        currentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
        revalidate();
        repaint();
    }

    ReportViewMode getViewMode() {
        return viewMode;
    }

    void setViewMode(ReportViewMode viewMode) {
        this.viewMode = viewMode;
        revalidate();
        repaint();
    }

    /**
     * 페이지별 보기에서 보여줄 페이지를 바꾼다.
     */
    void setCurrentPage(int page) {
        currentPage = Math.max(0, Math.min(page, getPageCount() - 1));
        revalidate();
        repaint();
    }

    int getCurrentPage() {
        return currentPage;
    }

    ReportZoomMode getZoomMode() {
        return zoomMode;
    }

    int getZoomPercent() {
        return zoomPercent;
    }

    void setZoom(ReportZoomMode zoomMode, int zoomPercent) {
        this.zoomMode = zoomMode;
        this.zoomPercent = clampPercent(zoomPercent);
        revalidate();
        repaint();
    }

    static int clampPercent(int percent) {
        return Math.max(ReportSettingConstraints.ZOOM_MIN, Math.min(ReportSettingConstraints.ZOOM_MAX, percent));
    }

    /**
     * 지금 쓰는 배율(화면 픽셀/pt). 맞춤 방식이어도 허용 범위(25% ~ 400%)를 벗어나지 않는다.
     */
    double getScale() {
        Dimension extent = viewportExtent();
        double scale = switch (zoomMode) {
        case FIT_WIDTH -> fitWidthScale(extent);
        case FIT_PAGE -> Math.min(fitWidthScale(extent), (extent.height - PAGE_GAP * 2) / ReportPaper.HEIGHT);
        case CUSTOM -> zoomPercent / 100.0 * PIXELS_PER_POINT;
        };
        double min = ReportSettingConstraints.ZOOM_MIN / 100.0 * PIXELS_PER_POINT;
        double max = ReportSettingConstraints.ZOOM_MAX / 100.0 * PIXELS_PER_POINT;
        return Math.max(min, Math.min(max, scale));
    }

    /**
     * 지금 쓰는 배율을 백분율로 나타낸 값. 맞춤 방식에서 확대/축소 버튼을 누를 때의 출발점이다.
     */
    int getEffectivePercent() {
        return (int) Math.round(getScale() / PIXELS_PER_POINT * 100);
    }

    private static double fitWidthScale(Dimension extent) {
        return (extent.width - PAGE_GAP * 2) / ReportPaper.WIDTH;
    }

    private Dimension viewportExtent() {
        if (getParent() instanceof JViewport viewport && viewport.getWidth() > 0) {
            return viewport.getExtentSize();
        }
        return FALLBACK_EXTENT;
    }

    private double pageWidth() {
        return ReportPaper.WIDTH * getScale();
    }

    private double pageHeight() {
        return ReportPaper.HEIGHT * getScale();
    }

    /**
     * 페이지의 왼쪽 끝. 페이지가 영역보다 좁으면 가운데에 놓는다.
     */
    private double pageLeft() {
        return Math.max(PAGE_GAP, (getWidth() - pageWidth()) / 2);
    }

    /**
     * 페이지의 위쪽 끝. 페이지별 보기에서는 현재 페이지만 있으며, 영역보다 낮으면 세로로도 가운데에 놓는다.
     */
    private double pageTop(int page) {
        if (viewMode == ReportViewMode.SINGLE_PAGE) {
            return Math.max(PAGE_GAP, (getHeight() - pageHeight()) / 2);
        }
        return PAGE_GAP + page * (pageHeight() + PAGE_GAP);
    }

    /**
     * 이어 보기에서 그 페이지의 맨 위가 영역의 맨 위에 오도록 할 때의 세로 스크롤 위치
     */
    int scrollTopOf(int page) {
        if (viewMode == ReportViewMode.SINGLE_PAGE) {
            return 0;
        }
        return (int) Math.round(pageTop(page) - PAGE_GAP);
    }

    /**
     * 세로 위치 y에 걸친 페이지. 이어 보기에서 페이지와 페이지 사이의 간격은 위쪽 페이지에 속한 것으로 본다.
     */
    int pageAtY(double y) {
        int count = getPageCount();
        if (count == 0) {
            return -1;
        }
        if (viewMode == ReportViewMode.SINGLE_PAGE) {
            return currentPage;
        }
        int page = (int) Math.floor((y - PAGE_GAP) / (pageHeight() + PAGE_GAP));
        return Math.max(0, Math.min(count - 1, page));
    }

    /**
     * 점이 종이 위에 있는지 여부. 종이 위에서만 끌어서 스크롤할 수 있다.
     */
    boolean isOnPaper(Point point) {
        int page = pageAtY(point.y);
        if (page < 0) {
            return false;
        }
        double left = pageLeft();
        double top = pageTop(page);
        return point.x >= left && point.x <= left + pageWidth() && point.y >= top && point.y <= top + pageHeight();
    }

    PagePoint toPagePoint(Point point) {
        int page = Math.max(0, pageAtY(point.y));
        double scale = getScale();
        return new PagePoint(page, (point.x - pageLeft()) / scale, (point.y - pageTop(page)) / scale);
    }

    /**
     * {@link #toPagePoint}의 반대. 지금의 배율과 크기로 계산하므로 배치가 끝난 뒤에 불러야 한다.
     */
    Point toViewPoint(PagePoint pagePoint) {
        double scale = getScale();
        return new Point((int) Math.round(pageLeft() + pagePoint.x() * scale),
                (int) Math.round(pageTop(pagePoint.page()) + pagePoint.y() * scale));
    }

    @Override
    public Dimension getPreferredSize() {
        if (document == null) {
            return new Dimension(0, 0);
        }
        // 맞춤 배율에서 소수 오차로 1픽셀이 넘쳐 스크롤 막대가 생기지 않게 조금 덜어 올림한다.
        int width = (int) Math.ceil(pageWidth() + PAGE_GAP * 2 - 0.01);
        int pages = viewMode == ReportViewMode.SINGLE_PAGE ? 1 : document.getPageCount();
        int height = (int) Math.ceil(PAGE_GAP + pages * (pageHeight() + PAGE_GAP) - 0.01);
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (document == null) {
            return;
        }

        double scale = getScale();
        double pageWidth = pageWidth();
        double pageHeight = pageHeight();
        double left = pageLeft();
        Rectangle clip = g.getClipBounds();

        int first = viewMode == ReportViewMode.SINGLE_PAGE ? currentPage : 0;
        int last = viewMode == ReportViewMode.SINGLE_PAGE ? currentPage : document.getPageCount() - 1;
        for (int i = first; i <= last; i++) {
            double top = pageTop(i);
            if (clip != null && (top + pageHeight < clip.y || top > clip.y + clip.height)) {
                continue;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(left, top);
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

    /**
     * 페이지가 영역보다 좁으면 영역 폭에 맞춰 늘어나 페이지를 가운데에 놓고, 넓으면 원래 폭을 유지해 가로 스크롤이 생기게 한다.
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport viewport && viewport.getWidth() >= getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
    }
}
